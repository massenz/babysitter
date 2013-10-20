# Simple server copyright (c) 2013 RiverMeadow software. All rights reserved.
# Created by M. Massenzio, marco@rivermeadow.com, 2013-10-03

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
        res['payload'] = self.payload
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
            # TODO: implement the retry logic using Kazoo facilities
            self._zk.start(timeout=NannyState.DEFAULT_TIMEOUT)
        except Exception, e:
            logging.error('Timeout trying to connect to a ZK ensemble [{}]'.format(e))
            # TODO: maybe a more graceful exit?
            exit(1)

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
        except Exception, e:
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


def parse_args():
    """ Implements the CLI activating the server registration via command line configuration

        Use --help to see the available options
    """
    parser = argparse.ArgumentParser()
    parser.add_argument('--type', '-t', default='simpleserver',
                        help='The type of server that is being monitored (useful when providing '
                             'diagnostics and stats)')
    parser.add_argument('--desc', '-d', default='A simple server to test monitoring',
                        help='A brief description, if any, to help with diagnostics and '
                             'statistics')
    parser.add_argument('--hosts', default='localhost:2181',
                        help='The Monitoring service hosts as a comma-separated list ('
                             'e.g.: "localhost:2181, 10.10.0.100:2182, monitor.net:2282")')
    parser.add_argument('--interval', '-i', default=60, type=int,
                        help='The interval for each update to be sent to the Monitoring service')
    parser.add_argument('--suffix', '-s',
                        help='A unique suffix to identify uniquely a server, if more than one '
                             'instance is running off the same host (this is obviously only '
                             'realistic for a dev/test environment, otherwise the hostname should '
                             'suffice)')
    return parser.parse_args()


def main():
    fmt = '%(asctime)-15s %(message)s'
    logging.basicConfig(format=fmt, level='DEBUG')
    pid = os.getpid()
    logging.info('This Nanny PID is {pid}'.format(pid=pid))
    conf = parse_args()
    if not conf.suffix:
        # This is just so we have a unique "hostname" if more than one simpleserver is
        # running on the same VM
        # in a real system, we would just use os.gethostname()
        setattr(conf, 'suffix', str(pid))
    logging.info('Registering server with babysitter service')
    nanny = NannyState(zk_hosts=conf.hosts, suffix=conf.suffix)
    logging.info('Starting infinite loop, hit Ctrl-C to terminate...')
    server = MonitoredServer(conf.type, conf.desc)
    nanny.register(server)
    while True:
        try:
            time.sleep(conf.interval)
            server.payload = {'current_time': time.asctime(),
                              'state': 'running'}
            nanny.update(server)
        except KeyboardInterrupt:
            server.payload = {'current_time': time.asctime(),
                              'state': 'shutdown'}
            nanny.update(server)
            logging.info('Terminating - this should also trigger an alert')
            break


if __name__ == '__main__':
    main()
