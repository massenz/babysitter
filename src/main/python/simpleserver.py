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


""" Simple server, example usage of the nanny.NannyState class

    Connects to a ZooKeeper instance(s) listening on the --hosts (by default, localhost:2181)
    and regularly updates with a trivial payload (the current time).

    Provides a sample usage of the L{NannyState} class.

@author: Marco Massenzio (m.massenzio@gmail.com)
         Created October, 2013
"""

import argparse
import logging
import os
import time
import nanny


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
        # running on the same VM: in a real system, we would just use the host name
        setattr(conf, 'suffix', str(pid))
    logging.info('Registering server with babysitter service')
    service = nanny.NannyState(zk_hosts=conf.hosts, suffix=conf.suffix)
    logging.info('Starting infinite loop, hit Ctrl-C to terminate...')
    server = nanny.MonitoredServer(conf.type, conf.desc)
    service.register(server)
    while True:
        try:
            time.sleep(conf.interval)
            server.payload = {'current_time': time.asctime(),
                              'state': 'running'}
            service.update(server)
        except KeyboardInterrupt:
            server.payload = {'current_time': time.asctime(),
                              'state': 'shutdown'}
            service.update(server)
            logging.info('Terminating - this should also trigger an alert')
            break

if __name__ == "__main__":
    main()
