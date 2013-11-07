# Copyright AlertAvert.com (c) 2013. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

""" Unit tests for the L{nanny.NannyState} class

@author: Marco Massenzio (m.massenzio@gmail.com)
         Created October, 2013
"""
import argparse

import json
import logging
import mock
import socket
import unittest

import nanny


class TestNannyState(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        fmt = '%(asctime)-15s %(message)s'
        logging.basicConfig(format=fmt, level='DEBUG')

    def setUp(self):
        self.hostname = socket.gethostname()
        self.ip = socket.gethostbyname(socket.gethostname())
        self.path = '/'.join([nanny.NannyState.ZK_TREE_ROOT, self.hostname])
        self._server = nanny.MonitoredServer('test', self.hostname, 'A unit test server')

    @mock.patch('kazoo.client.KazooClient')
    def test_register(self, mock_client):
        grabbed_data = dict()

        def grab_data(path, value, ephemeral, makepath):
            grabbed_data['value'] = value
            return self.path
        instance = mock_client.return_value
        instance.start = mock.MagicMock()
        instance.create = mock.MagicMock(side_effect=grab_data)
        instance.create.return_value = self.path
        ns = nanny.NannyState()
        instance.start.assert_called_once_with(timeout=nanny.NannyState.DEFAULT_TIMEOUT)
        real_path = ns.register(self._server)
        self.assertEqual(self.path, real_path)
        data = json.loads(grabbed_data['value'])
        self.assertEqual(self.hostname, data.get('server_address')['hostname'])
        self.assertEqual(self.ip, data.get('server_address')['ip'])

    @staticmethod
    def _extract_path_payload(mock_set):
        args, kwargs = mock_set.call_args
        path = args[0]
        version = kwargs['version']
        value = kwargs['value']
        value_dict = json.loads(value)
        return path, value_dict.get('payload'), version


    @mock.patch('kazoo.client.KazooClient')
    def test_update_with_payload(self, mock_client):
        instance = mock_client.return_value
        instance.exists.return_value = argparse.Namespace(version=1)
        ns = nanny.NannyState()
        payload_sent = {'foo': 'baz', 'ter': 33, 'map': dict({'key': 'value'})}
        self._server.payload = payload_sent
        ns.update(self._server)
        #noinspection PyProtectedMember
        full_path = ns._build_fullpath(self._server)
        instance.exists.assert_called_once_with(full_path)
        path, payload, version = self._extract_path_payload(instance.set)
        self.assertEqual(full_path, path)
        self.assertEqual(1, version)
        self.assertDictEqual(payload_sent, payload)

    @mock.patch('kazoo.client.KazooClient')
    def test_update_no_payload(self, mock_client):
        instance = mock_client.return_value
        instance.exists.return_value = argparse.Namespace(version=1)
        ns = nanny.NannyState()
        ns.update(self._server)
        #noinspection PyProtectedMember
        full_path = ns._build_fullpath(self._server)
        instance.exists.assert_called_once_with(full_path)
        path, payload, version = self._extract_path_payload(instance.set)
        self.assertEqual(full_path, path)
        self.assertEqual(1, version)
        self.assertIsNone(payload)
