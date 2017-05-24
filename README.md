# Ecantonchiro - "Hundred-Handed Ones" - designed for the developers

(agentless) automation engine

This is actually in alpha stage, if you're not a developer it's not still ready for you.

At this stage there are already working scripts to provision server from digitalocean,
run commands to create user and run shell:
works from windows or macos or linux (unzip file and run) having an ssh linux server as destination

This is a Java project, so you don't need specific instruction to install it on each platform ;-P
(the first alpha release is available)


## Background
I run remote deployment installation test of kargo and kubeadm.
All is done provisioning centos 7.x servers for each job on digitalocean (I run tests on bare metal too)

Actually I do it running a java application from windows, macos or linux:
I need 'em to test kargo and kubeadm releases, before evaluate to use 'em in production.

The requirements of kargo are some pre-flight activities and ansible with specific version, while kubeadm needs only some preflight checks.

I've decided to put all in a new application where the install start with yaml configuration file like ansible:
this is Ecantonchiro.



## Overview
The application is in pre alpha stage:

(2017/05/17) remote install ansible (why not!), working examples, first alpha release 

(2017/05/05) actually only provisiong server from DigitalOcean is working with yaml configurations files, as described in the wiki 

[Roadmap](https://github.com/naarani/ecantonchiro/wiki/roadmap)

the actual features are to run remote scripted commands from ssh like:

1. provisioning servers from digitalocean

1. yum

1. shell

... now working on templates reusing jinja2 templates on java with jinjava



## Build jar

to build the project run:

    $ git clone https://github.com/naarani/ecantonchiro.git
    $ cd ecantonchiro
    $ gradle --task jarWithLib

it will create in the _build/libs/_ directory the main jar runnable with all the needed jars in the directory _lib_ 



## License

Apache 2.0 



## Main Dependencies 

1) JSCH (BSD-style license) Copyright (c) 2002-2015 Atsuhiko Yamanaka, JCraft,Inc. All rights reserved.

2) Apache jclouds, Apache license 2.0

3) yamlbean EsotericSoftware/yamlbeans is licensed under the MIT License

4) ansible-inventory-java MIT license (c) Andrea Scarpino

5) jinjava https://github.com/HubSpot/jinjava  Apache license 2.0 (c) Jeff Boulter


## Sponsor
IPERIONE SRL


