# TODO: insert copyright
import json
import logging
import socket

__author__ = 'marco'

import mock
import unittest

import nanny


class TestSimpleServer(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        FORMAT = '%(asctime)-15s %(message)s'
        logging.basicConfig(format=FORMAT, level='DEBUG')

    def setUp(self):
        self.hostname = socket.gethostname()
        self.ip = socket.gethostbyname(socket.gethostname())
        self.path = '/'.join([nanny.NannyState.ZK_TREE_ROOT, self.hostname])

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
        ns = nanny.NannyState('test', 'This is a test server')
        instance.start.assert_called_once_with(timeout=nanny.NannyState.DEFAULT_TIMEOUT)
        self.assertEqual(self.path, ns._path)
        data = json.loads(grabbed_data['value'])
        self.assertEqual(self.hostname, data.get('server_address')['hostname'])
        self.assertEqual(self.ip, data.get('server_address')['ip'])

    @mock.patch('kazoo.client.KazooClient')
    def test_payload(self, mock_client):
        ns = nanny.NannyState('test_payload')
        self.assertDictEqual({}, ns.payload)
        ns.payload = {'foo': 'baz', 'ter': 33, 'map': dict({'key': 'value'})}
        self.assertIsNotNone(ns.payload)
        self.assertDictEqual({'foo': 'baz', 'ter': 33, 'map': {'key': 'value'}}, ns.payload)

