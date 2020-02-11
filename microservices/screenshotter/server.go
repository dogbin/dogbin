package main

import (
	"context"
	"fmt"
	"github.com/chromedp/cdproto/emulation"
	"github.com/chromedp/chromedp"
	"github.com/valyala/fasthttp"
	"net/http"
	"os"
)

var host = os.Getenv("DOGBIN_HOST")

var chromeCtx context.Context

func screenshotHandler(ctx *fasthttp.RequestCtx) {
	var buf []byte
	err := chromedp.Run(chromeCtx, elementScreenshot(fmt.Sprintf("%s/%s", host, ctx.Path()), "#content", &buf))
	if err != nil {
		ctx.SetStatusCode(http.StatusInternalServerError)
		return
	}
	if _, err := ctx.Write(buf); err != nil {
		ctx.SetStatusCode(http.StatusInternalServerError)
		return
	}
	ctx.SetContentType("image/png")
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
