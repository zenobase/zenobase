SSL Certificate (via namecheap.com)
===============

Signing Request
---------------

# generate certificate signing request (csr)
openssl req -new -newkey rsa:2048 -keyout server.key -out server.csr
# z3nowhat

# for aws-lb:
openssl rsa -in server.key -text > server_rsa.key


Elastic Load Balancer
---------------------

Private Key: server_rsa.key
Public Key Certificate: STAR_zenobase_com.crt
Certificate Chain: PositiveSSLCA2.crt + AddTrustExternalCARoot.crt
