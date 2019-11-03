package main

import (
	"bytes"
	"fmt"
	"github.com/alecthomas/chroma"
	"github.com/alecthomas/chroma/formatters/html"
	"github.com/alecthomas/chroma/lexers"
	"github.com/alecthomas/chroma/styles"
	"github.com/valyala/fasthttp"
	"github.com/wI2L/jettison"
)

type HighlightResponse struct {
	Lang string `json:"lang"`
	Code string `json:"code"`
}

func highlightHandler(ctx *fasthttp.RequestCtx) {
	lang := string(ctx.Request.PostArgs().Peek("lang"))
	filename := string(ctx.Request.PostArgs().Peek("filename"))
	code := string(ctx.Request.PostArgs().Peek("code"))
	styleName := string(ctx.Request.PostArgs().Peek("style"))
	if styleName == "" {
		// TODO: port "dogbin" style to chroma
		styleName = "dracula"
	}
	var lexer chroma.Lexer
	if lang != "" {
		lexer = lexers.Get(lang)
	}
	if lexer == nil && filename != "" {
		lexer = lexers.Match(filename)
	}
	if lexer == nil {
		lexer = lexers.Analyse(code)
	}
	if lexer == nil {
		lexer = lexers.Fallback
	}
	lexer = chroma.Coalesce(lexer)
	tokens, err := lexer.Tokenise(nil, code)
	if err != nil {
		ctx.Error(fmt.Sprint(err), fasthttp.StatusInternalServerError)
		return
	}
	style := styles.Get(styleName)
	if style == nil {
		style = styles.Fallback
	}
	// TODO: move line number stuff here from dogbin/app
	formatter := html.New(html.WithClasses(), html.TabWidth(2))
	buf := bytes.Buffer{}
	err = formatter.Format(&buf, style, tokens)
	if err != nil {
		ctx.Error(fmt.Sprint(err), fasthttp.StatusInternalServerError)
		return
	}
	json, err := jettison.Marshal(HighlightResponse{
		Lang: lexer.Config().Name,
		Code: buf.String(),
	})
	if err != nil {
		ctx.Error(fmt.Sprint(err), fasthttp.StatusInternalServerError)
		return
	}
	ctx.SetContentType("application/json")
	ctx.SetBody(json)
}

func main() {
	if err := fasthttp.ListenAndServe(":8080", highlightHandler); err != nil {
		fmt.Println(err)
	}
}
