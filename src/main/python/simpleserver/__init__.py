# Simple server copyright (c) 2013 RiverMeadow software. All rights reserved.
# Created by M. Massenzio, marco@rivermeadow.com, 2013-10-03

import argparse
import json
import os
import socket
import threading

import requests


""" A simple server that sends a hearbeat every --interval seconds to a given monitoring service.
"""

REGISTER_URL = 'register'
BEAT_URL = 'beat'


# TODO: this is just for testing, the real body should be created from a template
def build_hb_body(interval, max_missed):
    body = {
        'server_address': {
            "ip": "192.168.2.53",
            "hostname": socket.gethostname()
        },
        'ttl': interval,
        'max_missed': max_missed,
        'type': 'simpleserver',
        'desc': 'A simple heartbeat server',
        'port': 9099
    }
    return body


def heartbeat(url, interval, max_missed):
    """ Sends a regular heartbeat to the destination URL

    @param url: the monitoring server URL
    @param interval: the interval in seconds to schedule a new heartbeat
    @param max_missed: the number of times a heartbeat can be missed before the server is
    """
    # TODO: both variables should be configuration variables
    timeout = 5
    retries = 3
    while retries:
        try:
            print 'Sending heartbeat...'
            headers = {'content-type': 'application/json'}
            r = requests.post(url, headers=headers,
                              data=json.dumps(build_hb_body(interval, max_missed)),
                              timeout=timeout)
            if not r.status_code == 200:
                print '[ERROR] Error encountered while pinging babysitter {url}: {error}'.format(
                    url=url,
                    error=r.status_code)
                retries -= 1
            else:
                print 'ok'
                break
        except requests.exceptions.Timeout:
            print '[ERROR] Server at {url} did not respond within the given timeout '  \
                  '[{timeout} seconds]'.format(url=url, timeout=timeout)
            retries -= 1
    else:
        print '[ERROR] Too many failures, giving up'
        return
    start_heartbeat(url, interval, max_missed)


def start_heartbeat(url, interval, max_missed):
    t = threading.Timer(interval, heartbeat, args=[url, interval, max_missed])
    t.daemon = True
    t.start()


def register_server(conf):
    url = '/'.join([conf.url, REGISTER_URL, '_'.join([socket.gethostname(), conf.suffix])])
    data = build_hb_body(conf.interval, conf.max_missed)
    headers = {'content-type': 'application/json'}
    resp = requests.post(url=url, data=json.dumps(data), headers=headers, timeout=5)
    if not resp.status_code == 200:
        raise RuntimeError('Unexpected response from Monitoring server: {}'.format(resp.text))


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('--url', required=True, help='The Monitoring service main URL')
    parser.add_argument('--interval', '-i', default=60, type=int,
                        help='The interval for each heartbeat to be sent to the Monitoring service'
                             ' (aka TTL)')
    parser.add_argument('--max_missed', '-f', default=5, type=int,
                        help='The maximum number of times the Monitor will allow this server '
                             'to miss its heartbeat, before declaring it unresponsive.  In other '
                             'words, if this server is unable to send a heartbeat for longer than '
                             '(max_missed x TTL) seconds, it will be deemed as being down.')
    parser.add_argument('--suffix', '-s',
                        help='A unique suffix to identify uniquely a server, if more than one '
                             'instance is running off the same host (this is obviously only '
                             'realistic for a dev environment, otherwise the hostname would suffice')
    return parser.parse_args()


if __name__ == '__main__':
    pid = os.getpid()
    print 'This Server PID is {pid}'.format(pid=pid)
    conf = parse_args()
    if not conf.suffix:
        # This is just so we have a unique "hostname" if more than one simpleserver is running on the same VM
        # in a real system, we would just use os.gethostname()
        setattr(conf, 'suffix', pid)
    print 'Registering server with Monitor service'
    register_server(conf)
    print 'Starting heartbeat to {url} every {interval} seconds'.format(url=conf.url,
                                                                        interval=conf.interval)
    start_heartbeat('/'.join([conf.url, BEAT_URL]), conf.interval, conf.max_missed)
