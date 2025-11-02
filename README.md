# CSC240-04Fall25
# CSC240 – Phase 2: Distributed APIs with Apache APISIX Gateway 

## Overview
This project implements a **multi-API architecture** connected through **Apache APISIX** as an API Gateway.  
It consists of three microservices written in Java:

| API | Port | Purpose |
|-----|------|----------|
| **Data API** | 7001 | Serves country and university data |
| **Class API** | 7002 | Aggregates / verifies class & data info |
| **UI API** | 7003 | Provides a frontend API for dashboards |
| **APISIX Gateway** | 9080 | Routes all external traffic |
| **APISIX Admin API** | 9180 | Used to configure routes |


### Start APISIX via Docker
Make sure Docker Desktop or Rancher Desktop is running, then from the repo root:

```bash
docker compose up -d

## Setup & Run Instructions
Run the Three API Servers (in separate terminals)

Data API:

javac -cp "lib/*" src/dataapi/DataApiServer.java -d out
then in seperate terminal:

java -cp "out;lib/*" dataapi.DataApiServer

Class API:

javac -cp "lib/*" src/classapi/ClassApiServer.java -d out
then in seperate terminal:

java -cp "out;lib/*" classapi.ClassApiServer

UI API:

javac -cp "lib/*" src/uiapi/UiApiServer.java -d out
then in seperate terminal:

java -cp "out;lib/*" uiapi.UiApiServer

### Powershell scripts to create routes

$HOST_IP = (Get-NetIPAddress -AddressFamily IPv4 `
            | Where-Object {$_.InterfaceAlias -match "Wi-Fi|Ethernet"} `
            | Select-Object -ExpandProperty IPAddress)[0]

$headers = @{
  "X-API-KEY"    = "edd1c9f034335f136f87ad84b625c8f1"
  "Content-Type" = "application/json"
}

# Route 1: Data API
$bodyData = @"
{
  "uri": "/data/*",
  "plugins": { "proxy-rewrite": { "regex_uri": ["^/data/(.*)", "/$1"] } },
  "upstream": {
    "type": "roundrobin",
    "scheme": "http",
    "nodes": [ { "host": "$HOST_IP", "port": 7001, "weight": 1 } ]
  }
}
"@
Invoke-RestMethod -Method Put -Uri "http://localhost:9180/apisix/admin/routes/1" -Headers $headers -Body $bodyData

# Route 2: Class API
$bodyClass = @"
{
  "uri": "/api/*",
  "plugins": { "proxy-rewrite": { "regex_uri": ["^/api/(.*)", "/$1"] } },
  "upstream": {
    "type": "roundrobin",
    "scheme": "http",
    "nodes": [ { "host": "$HOST_IP", "port": 7002, "weight": 1 } ]
  }
}
"@
Invoke-RestMethod -Method Put -Uri "http://localhost:9180/apisix/admin/routes/2" -Headers $headers -Body $bodyClass

# Route 3: UI API
$bodyUI = @"
{
  "uri": "/ui/api/*",
  "plugins": { "proxy-rewrite": { "regex_uri": ["^/ui/api/(.*)", "/api/$1"] } },
  "upstream": {
    "type": "roundrobin",
    "scheme": "http",
    "nodes": [ { "host": "$HOST_IP", "port": 7003, "weight": 1 } ]
  }
}
"@
Invoke-RestMethod -Method Put -Uri "http://localhost:9180/apisix/admin/routes/3" -Headers $headers -Body $bodyUI

```
Should show something like: 
<img width="1250" height="325" alt="Working " src="https://github.com/user-attachments/assets/8fe64b1f-509b-4ccd-b296-92f38714bc64" />

Server Run Commands after routing shows up 

Example Commands:  "curl http://localhost:9080/data/countries"
Example Commands:  "curl http://localhost:9080/api/health"
Example Commands:  "curl http://localhost:9080/ui/dashboard"


<img width="1596" height="934" alt="Working ClassAPI route" src="https://github.com/user-attachments/assets/f2322c61-c8d0-4beb-8df8-41567a47b081" />

