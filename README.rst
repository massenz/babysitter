==============================
Babysitter - Monitoring server
==============================

.. image:: https://go-shields.herokuapp.com/license-apache2-blue.png

:Date: 2013-10-07
:Author: M. Massenzio
:Version: 0.2
:Updated: 2014-01-24

Use Case
--------

We want an application-level monitoring system that enables a pluggable
alerting system to trigger alerts when ``nodes`` are terminated (for
whatever reason, planned or unplanned).

The monitoring system is "application unaware" in that it does not know any of
the details of any of the services that may be using it, so it is widely applicable
to existing, and future, services, without modifying either the service nodes
themselves, or the monitoring system when new services are added to the "monitored
pool."

Further, the Monitoring Service will allow arbitrary `Plugins`_ to be added, and
removed, by defining a simple interface.

Zookeeper_ will be used as the main node state tracker.

Architecture
------------

The system is designed to be fully distributed, highly available and resilient to
failures: it can be deployed in "standalone" (one server) mode, or as a set of identical
monitoring nodes; the nodes are entirely unaware of each other's presence, and will continue
to function in the event of failures of any one (or many) of them.

In the diagram below, the dotted box marked `babysitter` should be assumed to be replicated
many times, possibly across data centers - provided the Zookeeper_ ensemble they connect to
shares state.

.. image:: docs/images/babysitter.png
    :width: 400px
    :alt: babysitter architecture

The box ``AlertPlugin`` represents one (of many) plugin components that implement the
``Pager`` interface and the ``AlertPlugin`` API specification: not all Babysitter nodes
must have the same set of plugins, however, as only **one** node will trigger an alert in
the event of a monitored node failure, if the given plugin(s) is not installed on that **one**
server, no alert will be generated on that particular adapter.

Client API
^^^^^^^^^^

Plugins
^^^^^^^


Components
----------

Configuration
-------------

  -Dplugin.config_path="${HOME}/.babysitter/etc"

Application
^^^^^^^^^^^

Example ``application.properties`` file::

    # Configuration for babysitter service

    zookeeper.hosts: localhost:2181,localhost:2182,localhost:2183
    zookeeper.session_timeout: 5000

    # This is the path where all the servers will be attached, as children
    zookeeper.base_path: /monitor/hosts

    # Alerts will be appended as children of this node:
    zookeeper.alerts_path: /monitor/alerts

    # A common place to store configuration information
    zookeeper.config_path: /monitor/config

    server.port: 9000
    bootstrap.location: classpath:/bootstrap.json
    plugin.config_path: /tmp/plugins/config

    # Maximum delay before an AlertManager triggers an alert, in msec
    # currently not used
    alert.max_delay_msec: 5000

Most of the configuration properties can be defined on the command line too, via
a system property variable::

    -Dserver.port=9001 -Dboostrap.location=file:///etc/babysitter/bootstrap.json


Bootstrapping
^^^^^^^^^^^^^

If ``bootstrap.location`` is defined, the file will be loaded and the
specified nodes created in ZK::

    {
        "paths": [
            "/monitor",
            "/monitor/hosts",
            "/monitor/config",
            "/monitor/alerts"
        ]
    }

The number and location of nodes created is entirely arbitrary, but it must at least
ensure that the nodes defined in the ``zookeeper.base_path`` and ``zookeeper.alerts_path``
are created (``zookeeper.config_path`` is currently not used, but it is recommended that
that node is created too).

The location of the file can, as usual, be defined via a system property too::

    -Dbootstrap.location=file:///etc/babysitter/conf/bootstrap.json

Logging
^^^^^^^

We use log4j_ for logging, the configuration follows the standard pattern: a default
``log4j.properties`` is in the classpath (``/src/main/resources/log4j.properties``)::

    # Root logger option
    log4j.rootLogger=DEBUG, stdout

    # Direct log messages to stdout
    log4j.appender.stdout=org.apache.log4j.ConsoleAppender
    log4j.appender.stdout.Target=System.out
    log4j.appender.stdout.layout=org.apache.log4j.PatternLayout

    # WARNING - this is suitable for development/debug, but NOT for production, please replace
    # in production environments with a less expensive pattern layout
    log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %C{1}.%M:%L - %m%n

    # TODO: add a RollingFileAppender

    log4j.logger.com.rivermeadow = DEBUG
    log4j.logger.org = INFO

This can be changed, specifying the location of the logging configuration file via a system
property::

    -Djava.util.logging.config.file="/etc/babysitter/conf/logging.properties"


Protocol
--------

Server object (``JSON``)::

    {
        "server_address": {
            "ip": "192.168.1.61",
            "hostname": "Marcos-MacBook-Pro.local"
        },
        "type": "simpleserver",
        "port": 0,
        "payload": {
            "current_time": "Wed Nov 6 23:30:53 2013",
            "state": "running"
        },
        "desc": "A simple server to test monitoring"
    }


.. Links:

.. _Zookeeper: http://zookeeper.apache.org/
.. _log4j:
