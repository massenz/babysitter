# Simple server copyright (c) 2013 RiverMeadow software. All rights reserved.
# Created by M. Massenzio, marco@rivermeadow.com, 2013-10-03

import argparse
import json
import logging
import os
import socket
import time

from kazoo.client import KazooClient
from kazoo.interfaces import IHandler


class NannyState(object):
    """ A simple server that sends a hearbeat every --interval seconds to a given monitoring
        service.
    """

    DEFAULT_TIMEOUT = 10
    #: timeout for a server connection

    ZK_TREE_ROOT = '/monitor/hosts'
    #: the node where the monitoring subtree is rooted

    def __init__(self, zk_hosts='localhost:2181', interval=30, suffix=None, port=0):
        """ Creates the nanny and starts the ZK client and registers this host to be babysat

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
        self._zk = KazooClient(hosts=zk_hosts, timeout=NannyState.DEFAULT_TIMEOUT)
        try:
            # TODO: implement the retry logic using Kazoo facilities
            self._zk.start(timeout=NannyState.DEFAULT_TIMEOUT)
            self.register()
        except IHandler.timeout_exception:
            logging.error('Timeout trying to connect to a ZK ensemble')
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
            'type': 'simpleserver',
            'desc': 'A simple heartbeat server',
            'port': self.port
        }
        return body

    def register(self):
        """ Creates a new node for this host in the Zk tree
        """
        path = '/'.join([NannyState.ZK_TREE_ROOT, self._hostname])
        value = json.dumps(self._build_hb_body())
        try:
            real_path = self._zk.create(path, value=value, ephemeral=True, makepath=True)
            logging.info('Server registered with ZK: {}'.format(real_path))
        except Exception, e:
            logging.error('Could not create node {}: {}'.format(path, e))

    # TODO: implement the regular node update

    @staticmethod
    def _get_my_ip():
        """ Hackish way to obtain the machine's IP address, will fail for multiple NICs
        """
        return socket.gethostbyname_ex(socket.gethostname())


def parse_args():
    """ Implements the CLI activating the server registration via command line configuration

        Use --help to see the available options
    """
    parser = argparse.ArgumentParser()
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
    nanny = NannyState(conf.hosts, conf.interval, conf.suffix)
    while True:
        time.sleep(2)
    logging.info('Terminating - this should also trigger an alert')
    # TODO: figure out a protocol to indicate this is a planned shutdown


if __name__ == '__main__':
    main()
