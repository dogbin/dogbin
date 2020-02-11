package main

import (
	"bytes"
	"context"
	"fmt"
	"github.com/allegro/bigcache"
	"github.com/chromedp/cdproto/emulation"
	"github.com/chromedp/chromedp"
	"github.com/minio/minio-go"
	"github.com/valyala/fasthttp"
	"log"
	"os"
	"strconv"
	"time"
)

var cache, _ = bigcache.NewBigCache(bigcache.DefaultConfig(12 * time.Hour))

var host = os.Getenv("DOGBIN_HOST")

var (
	s3Endpoint  = os.Getenv("S3_ENDPOINT")
	s3AccessKey = os.Getenv("S3_ACCESS_KEY")
	s3Secret    = os.Getenv("S3_SECRET")
	s3Secure, _ = strconv.ParseBool(os.Getenv("S3_SECURE"))
	s3Bucket    = os.Getenv("S3_BUCKET")
	s3Region    = os.Getenv("S3_REGION")
	s3Host      = os.Getenv("S3_HOST")
)

var minioClient *minio.Client

func init() {
	var err error
	minioClient, err = minio.NewWithRegion(s3Endpoint, s3AccessKey, s3Secret, s3Secure, s3Region)
	if err != nil {
		log.Fatalln(err)
	}

	exists, err := minioClient.BucketExists(s3Bucket)
	if !exists {
		log.Fatalln(err)
	}

	err = minioClient.SetBucketLifecycle(s3Bucket, `<LifecycleConfiguration xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
  <Rule>
    <ID>Expire old screenshots</ID>
    <Prefix>screenshots/</Prefix>
    <Status>Enabled</Status>
    <Expiration>
      <Days>30</Days>
    </Expiration>
  </Rule>

  <Rule>
    <ID>Remove uncompleted uploads</ID>
    <Status>Enabled</Status>
    <Prefix/>
    <AbortIncompleteMultipartUpload>
      <DaysAfterInitiation>1</DaysAfterInitiation>
    </AbortIncompleteMultipartUpload>
  </Rule>
</LifecycleConfiguration>`)
	if err != nil {
		log.Println(err)
	}
}

var chromeCtx context.Context

func screenshotHandler(ctx *fasthttp.RequestCtx) {
	objName := fmt.Sprintf("screenshots%s.png", ctx.Path())
	version := ctx.QueryArgs().GetUintOrZero("v")
	cacheValue, err := cache.Get(objName)

	ctx.WriteString(fmt.Sprintf("%s/%s?v=%d", s3Host, objName, version))

	if err != nil || len(cacheValue) > 0 && int(cacheValue[0]) < version {
		go func(path []byte, objName string, version int) {
			var buf []byte
			url := fmt.Sprintf("%s%s", host, path)
			fmt.Println(url)
			err := chromedp.Run(chromeCtx, elementScreenshot(url, "#content", &buf))
			if err != nil {
				log.Println(err)
				return
			}
			_, err = minioClient.PutObject(s3Bucket, objName, bytes.NewReader(buf), int64(len(buf)), minio.PutObjectOptions{
				UserMetadata: map[string]string{"x-amz-acl": "public-read"},
				ContentType:  "image/png",
			})
			if err != nil {
				log.Println(err)
			}
			cache.Set(objName, []byte{byte(version)})
		}(ctx.Path(), objName, version)
	}
}

// elementScreenshot takes a screenshot of a specific element.
func elementScreenshot(urlstr, sel string, res *[]byte) chromedp.Tasks {
	return chromedp.Tasks{
		chromedp.Navigate(urlstr),
		chromedp.WaitVisible(sel, chromedp.ByID),
		chromedp.Screenshot(sel, res, chromedp.NodeVisible, chromedp.ByID),
	}
}

func main() {
	fmt.Println("dogbin - screenshotter (v1.something)")
	fmt.Println("~ Connecting to chrome...")
	var cancel func()
	chromeCtx, cancel = chromedp.NewContext(context.Background())
	defer cancel()
	chromedp.Run(chromeCtx, emulation.SetDeviceMetricsOverride(1080, 720, 1, false))

	fmt.Println("~ Started listening on port 8082")
	if err := fasthttp.ListenAndServe(":8082", screenshotHandler); err != nil {
		fmt.Println(err)
	}
}
