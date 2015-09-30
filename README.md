##AgentDroid

>_be the change you wish to see in the world_

__AgentDroid is an Android client for GoAgent PHP.__


## What it does

Working with GoAgent, __AgentDroid bypass network blocking__ through the following steps: 

1.Setup a HTTP/HTTPS proxy in Android, any HTTP(S) traffic of your choice will be redirected transparently to the proxy. 

2.Then the proxy will encode the HTTP(S) data, and send them to the PHP script on the server side.

_2.5.In HTTPS's case, the proxy will do a MITM and act as the HTTPS server itself._

3.PHP script decodes the data and make the original HTTP(S) requests (hence the hosting service must supports CURL).

4.PHP script encodes the data and returns them to AgentDroid.

####In general:

Apps <-> [ Iptables <-> AgentDroid Java Codes ]* <-> GoAgent PHP script

*_these are inside AgentDroid_

## How to use

_Rerequisites_:

_Your phone must be rooted and its build version is greater than 4.0._

1. Find a php hosting site which __supports CURL__ (just google "php hosting curl");
2. Register and login;
3. Upload the __index.php__ file in the [GoAgent](https://github.com/goagent/goagent) project to any directory in your site; Or, since now GoAgent project is closed, you can use the __agentdroid.php__ file in the php folder _NOTE: AgentDroid must work with Goagent v3.1.2_
 
4. The __test\_curl.php__ script in the php folder is to help you check whether your php host is capable to deliver your request: Upload test_curl.php, and in your browser access the page http://your\_php\_host/test\_curl.php?url=http://www.handhandlab.com, if browser shows the content of handhandlab.com, then your host is good to go!

5. Open app and set the url of the index.php file, and set which apps should be proxied.
6. Start the proxied app and use it.


## What may interest you

For those who doesn't need to "cross the great firewall", you may also be benefited from this project, as it provides sample Java codes of the following features:

* Programetically generating a CA and let Android trust it.

* Generating a certificate and signing it with the CA on the fly.

* Man in the middle attack(MITM) in Android.

* SSLEngine in Android

* Making a Http/Https proxy by Java NIO in Android


Although some of the implementations may be simple or even buggy, but they demostrate the basic idea, and most of all, with working codes.

## 中文说明

GoAgent是一个科学上网的工具，大部分人都使用它的AppEngine端的功能，而PHP端确实被忽略的一大利器。

Google AppEngine目标固定，容易被blocked，相反，提供PHP网页寄存（Hosting）服务的网站多如牛毛，而且经常有新的提供商出来，使得伟大的火墙很难把它们一网打尽。

AgentDroid即为一个与GoAgent PHP端通信的Android客户端APP。

## Acknowledgment

AgentDroid is depending on the following excellent projects:

* [GoAgent](https://github.com/goagent/goagent)
* [Openssl](https://github.com/openssl/openssl)

Also, AgentDroid can not be made without many other great blogs and websites, I will list them on the corresponding WIKI pages.

## Major Updates

####2015.09.30
* Use native openssl to generate CA certificate, key pairs and certificates.

