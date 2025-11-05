# spring boot run

mvn spring-boot:run 


# sample request

```
POST /api/upload HTTP/1.1
Content-Length: 65078
Content-Type: multipart/form-data; boundary=PieBoundary123456789012345678901234567
Host: 192.168.0.136:8058
User-Agent: HTTPie

--PieBoundary123456789012345678901234567
Content-Disposition: form-data; name="reportId"

2
--PieBoundary123456789012345678901234567
Content-Disposition: form-data; name="file"; filename="japan.zip"
Content-Type: application/zip


--PieBoundary123456789012345678901234567--
```