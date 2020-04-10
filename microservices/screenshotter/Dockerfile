FROM golang as build-env
COPY . /screenshotter
WORKDIR /screenshotter
RUN ["go", "build", "-tags", "netgo"]

FROM chromedp/headless-shell:latest
RUN apt-get update \
  && apt-get install --no-install-recommends -qy tini \
  && rm -rf /var/lib/apt/lists/*
COPY --from=build-env /screenshotter/screenshotter /screenshotter
COPY --from=build-env /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
EXPOSE 8082
ENTRYPOINT ["tini"]
CMD ["/screenshotter"]
