SSL Certificate (via namecheap.com)
===============

# generate certificate signing request (csr)
openssl req -new -newkey rsa:2048 -keyout server.key -out server.csr
# z3nowhat

# for aws-lb:
openssl rsa -in server.key -text
