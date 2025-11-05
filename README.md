# Spring Boot ZIP Upload API

Simple Spring Boot service that accepts ZIP uploads, validates the payload, and stores the file on disk.

## Prerequisites
- JDK 17 or later
- Maven 3.9+ (`mvn` must be on your `PATH`)

## Run locally
```bash
mvn spring-boot:run
```
The application listens on `http://localhost:8058` (configurable via `server.port` in `src/main/resources/application.properties`).

## Upload endpoint
- Method & path: `POST /api/upload`
- Content type: `multipart/form-data`
- Required parts:
  - `file`: ZIP archive (`application/zip` or `.zip` extension)
  - `reportId`: string identifier used when naming the stored file

### Sample request (HTTPie)
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

### Sample JSON response
```json
{
  "path": "/tmp/2-japan.zip"
}
```

## File storage
Uploaded archives are written to `/tmp` by default (see `TARGET_DIRECTORY` in `src/main/java/com/example/fileupload/FileUploadController.java`). The filename combines the sanitized `reportId` and original filename.

To use a different directory, update the `TARGET_DIRECTORY` constant or replace it with a configurable property before building.

## Troubleshooting
- `java.nio.file.AccessDeniedException: /temp`: The application could not write to the target directory. Ensure the directory exists and is writable, or adjust `TARGET_DIRECTORY` to point to a path you control (e.g. `/tmp/uploads`).
