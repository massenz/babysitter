# Simple server copyright (c) 2013 RiverMeadow software. All rights reserved.
# Created by M. Massenzio, marco@rivermeadow.com, 2013-10-03

import argparse
import json
import logging
import os
import socket
import time

import kazoo.client
from kazoo.interfaces import IHandler


class NannyState(object):
    """ A simple server that sends a hearbeat every --interval seconds to a given monitoring
        service.
    """

    DEFAULT_TIMEOUT = 10
    #: timeout for a server connection

    ZK_TREE_ROOT = '/monitor/hosts'
    #: the node where the monitoring subtree is rooted

    def __init__(self, server_type, desc=None, zk_hosts='localhost:2181', interval=30,
                 suffix=None, port=0):
        """ Creates the nanny and starts the ZK client and registers this host to be babysat

        Upon registration (at construction) only the basic details for the server are sent to the
        ZK service, then, if the ``set_payload()`` is called, the new data is sent to ZK to be
        associated with the server: this can be done several times over the life of the server.

        @param zk_hosts: a comma-separated list of host:port for the ZooKeeper service
        @param interval: the interval to send updates to ZK (in seconds)
        @type interval: int
        @param suffix: an optional suffix for the hostname to be sent
        @param port: an optional port number, if this guards a server listening on a specific port
        @type port: int
        """
        self.interval = interval
        self.port = port
        self._suffix = suffix
        self._ip = self._get_my_ip()
        self._hostname = socket.gethostname() if not suffix else '_'.join([socket.gethostname(),
                                                                           suffix])
        self.server_type = server_type
        self.desc = desc
        self._payload = dict()
        self._zk = kazoo.client.KazooClient(hosts=zk_hosts, timeout=NannyState.DEFAULT_TIMEOUT)
        try:
            # TODO: implement the retry logic using Kazoo facilities
            self._zk.start(timeout=NannyState.DEFAULT_TIMEOUT)
            self._path = self.register()
        except Exception, e:
            logging.error('Timeout trying to connect to a ZK ensemble [{}]'.format(e))
            # TODO: maybe a more graceful exit?
            exit(1)

    # TODO: this is just for testing, the real body should be created from a template
    def _build_hb_body(self):
        body = {
            'server_address': {
                "ip": self._ip,
                "hostname": self._hostname
            },
            'ttl': self.interval,
            'type': self.server_type,
            'desc': self.desc,
            'port': self.port,
            'payload': self._payload
        }
        return body

    def _get_path_value(self):
        path = '/'.join([NannyState.ZK_TREE_ROOT, self._hostname])
        value = json.dumps(self._build_hb_body())
        return path, value

    def register(self):
        """ Creates a new node for this host in the Zk tree
        """
        path, value = self._get_path_value()
        try:
            real_path = self._zk.create(path, value=value, ephemeral=True, makepath=True)
            logging.info('Server registered with ZK: {}'.format(real_path))
            return real_path
        except Exception, e:
            logging.error('Could not create node {}: {}'.format(path, e))
            raise e

    # TODO: implement the regular node update
    def update(self):
        pass

    @staticmethod
    def _get_my_ip():
        """ Hackish way to obtain the machine's IP address, will fail for multiple NICs
        """
        return socket.gethostbyname(socket.gethostname())

    @property
    def payload(self):
        return self._payload

    @payload.setter
    def payload(self, values):
        self.payload.update(values)

    @payload.deleter
    def payload(self):
        del self._payload


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
                        help='The interval for each heartbeat to be sent to the Monitoring service'
                             ' (aka TTL)')
    parser.add_argument('--suffix', '-s',
                        help='A unique suffix to identify uniquely a server, if more than one '
                             'instance is running off the same host (this is obviously only '
                             'realistic for a dev/test environment, otherwise the hostname should '
                             'suffice)')
    return parser.parse_args()


def main():
    FORMAT = '%(asctime)-15s %(message)s'
    logging.basicConfig(format=FORMAT, level='DEBUG')
    pid = os.getpid()
    logging.info('This Nanny PID is {pid}'.format(pid=pid))
    conf = parse_args()
    if not conf.suffix:
        # This is just so we have a unique "hostname" if more than one simpleserver is
        # running on the same VM
        # in a real system, we would just use os.gethostname()
        setattr(conf, 'suffix', str(pid))
    logging.info('Registering server with babysitter service')
    nanny = NannyState('demo', zk_hosts=conf.hosts, interval=conf.interval, suffix=conf.suffix)
    logging.info('Starting infinite loop, hit Ctrl-C to terminate...')
    while True:
        try:
            time.sleep(2)
        except KeyboardInterrupt:
            # TODO: figure out a protocol to indicate this is a planned shutdown
            logging.info('Terminating - this should also trigger an alert')
            break


if __name__ == '__main__':
    main()
