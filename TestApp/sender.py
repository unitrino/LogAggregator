import requests

r1 = requests.put("http://localhost:8080/channels/1", data = {"level":"info","source_ip":"12.12.12.12", "title":"test1","data":'''{"jsondata":"strings"}''',"tags":"t1"});
r2 = requests.put("http://localhost:8080/channels/1", data = {"level":"debug","source_ip":"12.12.12.13", "title":"test2","data":'''{"jsondata":"strings}"''',"tags":"t3,t4"});
r3 = requests.put("http://localhost:8080/channels/1", data = {"level":"debug","source_ip":"12.12.12.14", "title":"test2","data":'''{"jsondata":"strings}"''',"tags":"t3,t4"});
r4 = requests.put("http://localhost:8080/channels/1", data = {"level":"info","source_ip":"12.12.12.15", "title":"test1","data":'''{"jsondata":"strings}"''',"tags":"t2,t1"});
r5 = requests.put("http://localhost:8080/channels/1", data = {"level":"error","source_ip":"12.12.12.16", "title":"test2","data":'''{"jsondata":"strings}"''',"tags":"t5,t6"});
r6 = requests.put("http://localhost:8080/channels/1", data = {"level":"critical","source_ip":"12.12.12.17", "title":"test1","data":'''{"jsondata":"strings}"''',"tags":"t9,t99"});

r7 = requests.put("http://localhost:8080/channels/2", data = {"level":"critical","source_ip":"12.12.12.17", "title":"test1","data":'''{"jsondata2":"strings}"''',"tags":"tt"});
r8 = requests.put("http://localhost:8080/channels/2", data = {"level":"critical","source_ip":"12.12.12.17", "title":"test1","data":'''{"jsondata23":"strings}"''',"tags":"tt"});
r9 = requests.put("http://localhost:8080/channels/2", data = {"level":"critical","source_ip":"12.12.12.17", "title":"test1","data":'''{"jsondata3":"strings}"''',"tags":"t22"});
