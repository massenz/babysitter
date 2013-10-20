#!/usr/bin/env python
#
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

"""
@author: Marco Massenzio (m.massenzio@gmail.com)
         Created October, 2013
"""

import argparse
import json
import logging
import os
import socket
import time

import kazoo.client
import kazoo.exceptions


class MonitoredServer(object):
    """ Encapsulates the description of a server, and will be available to monitoring services

        IP and hostname are obtained directly from the underlying OS services, the payload must be
        set prior to registering the server and must be regularly updated as it changes (assuming
        that the information is of interest/use to the monitoring service(s)).
    """

    def __init__(self, server_type, desc=None, port=0):
        """
        @param server_type: the type of server, for the alert service to take specific actions
        depending on the type of server unexpectedly failing
        @type server_type: str
        @param desc: a brief description of this server, mostly to include in human-readable
        alert messages
        @type desc: str
        @param port: the port this server is listening to, if applicable
        @type port: int
        @rtype : MonitoredServer
        """
        self.server_type = server_type
        self.description = desc
        self._ip = self._get_my_ip()
        self.hostname = socket.gethostname()
        self.port = port
        self._payload = dict()

    @property
    def payload(self):
        return self._payload

    @payload.setter
    def payload(self, data):
        self._payload = data

    @staticmethod
    def _get_my_ip():
        """ Hackish way to obtain the machine's IP address, will fail for multiple NICs
        """
        return socket.gethostbyname(socket.gethostname())

    def _build_hb_body(self):
        """ Builds the basic representation of this server, without any payload data

        @return: a basic description for the server
        @rtype: dict
        """
        body = {
            'server_address': {
                "ip": self._ip,
                "hostname": self.hostname
            },
            'type': self.server_type,
            'desc': self.description,
            'port': self.port,
        }
        return body

    @property
    def info(self):
        """ Creates a full description of the server, including payload information

            This method can be overridden by derived classes, however should
        """
        res = self._build_hb_body()
        if self.payload:
            res['payload'] = self.payload
        elif 'payload' in res:
            res.pop('payload')
        return res


class NannyState(object):
    """ Base abstract class to attach a server process to a monitoring ZooKeeper instance
    """

    DEFAULT_TIMEOUT = 10
    #: timeout for a server connection

    ZK_TREE_ROOT = '/monitor/hosts'
    #: the node where the monitoring subtree is rooted

    def __init__(self, zk_hosts='localhost:2181', suffix=None):
        """ Creates the nanny and starts the ZK client and registers this host to be babysat

        Upon registration (at construction) only the basic details for the server are sent to the
        ZK service, then, if the ``set_payload()`` is called, the new data is sent to ZK to be
        associated with the server: this can be done several times over the life of the server.

        @param zk_hosts: a comma-separated list of host:port for the ZooKeeper service
        @param suffix: an optional suffix for the hostname to be sent
        """
        self._suffix = suffix
        self._zk = kazoo.client.KazooClient(hosts=zk_hosts, timeout=NannyState.DEFAULT_TIMEOUT)
        try:
            self._zk.start(timeout=NannyState.DEFAULT_TIMEOUT)
        except Exception as e:
            logging.error('Timeout trying to connect to a ZK ensemble [{}]'.format(e))
            raise RuntimeError('No Zookeeper ensemble available at {}'.format(zk_hosts))

    def _build_fullpath(self, server):
        node_name = '_'.join([server.hostname, self._suffix]) if self._suffix is not None else \
            server.hostname
        full_path = '/'.join([NannyState.ZK_TREE_ROOT, node_name])
        return full_path

    def register(self, server):
        """ Creates a new node for the ``server`` in the Zk tree

        @param server: the server being monitored
        @type server: L{MonitoredServer}

        @return: the full path of the registered node in ZK for this server
        @rtype: string

        @raise L{kazoo.exceptions.ZookeeperError} if server returns a non-zero error code.
        """
        path = self._build_fullpath(server)
        value = json.dumps(server.info)
        try:
            real_path = self._zk.create(path, value=value, ephemeral=True, makepath=True)
            logging.info('Server {} registered with ZK at {}'.format(server.hostname, real_path))
            return real_path
        except Exception as e:
            logging.error('Could not create node {}: {}'.format(path, e))
            raise e

    def update(self, server):
        """ Updates the data associated with the server node.

            The server **must** have been already registered using L{#register}

            @raise L{kazoo.exceptions.NoNodeError} if the node was not registered
            @raise L{kazoo.exceptions.BadVersionError if version doesn't match.
            @raise L{kazoo.exceptions.ZookeeperError} if server returns a non-zero error code.
        """
        node = self._build_fullpath(server)
        stat = self._zk.exists(node)
        if stat:
            new_version = stat.version
            self._zk.set(node, value=json.dumps(server.info), version=new_version)
        else:
            raise kazoo.exceptions.NoNodeError(
                'Server {} should be registered first'.format(node))
