# TODO: insert copyright
import json
import socket

__author__ = 'marco'

import mock
import unittest

import nanny


class TestSimpleServer(unittest.TestCase):
    @mock.patch('kazoo.client.KazooClient')
    def test_register(self, mock):
        ns = nanny.NannyState()
        print ns._zk
