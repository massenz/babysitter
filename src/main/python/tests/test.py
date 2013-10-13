# TODO: insert copyright
import json
import socket

__author__ = 'marco'

import argparse
import mock
import unittest

import simpleserver


class TestSimpleServer(unittest.TestCase):
    @mock.patch('requests.post')
    def test_register(self, post):
        ok = argparse.Namespace(status_code=200)
        post.return_value = ok
        config = argparse.Namespace(url='http://test.com',
                                    suffix='test',
                                    interval=30,
                                    max_missed=5)

        url = '/'.join([config.url, simpleserver.REGISTER_URL, '_'.join([socket
                                                                         .gethostname(),
                                                                         config.suffix])])
        data = simpleserver.build_hb_body(config.interval, config.max_missed)
        simpleserver.register_server(config)
        post.assert_called_once_with(url=url,
                                     data=json.dumps(data),
                                     headers={'content-type': 'application/json'},
                                     timeout=5)
