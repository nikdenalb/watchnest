# CMS API first: ^~ so /cms/api/ never falls through to the SPA.
# proxy_pass has no URI suffix — Spring sees /cms/api/v1/... unchanged.
location ^~ /cms/api/ {
    proxy_pass http://app:8080;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location = /cms/api {
    return 308 /cms/api/;
}

location = /cms {
    return 301 /cms/;
}

location /cms/ {
    root /usr/share/nginx/html;
    try_files $uri /cms/index.html;
}
