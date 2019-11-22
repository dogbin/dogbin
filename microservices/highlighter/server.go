package main

import (
	"bytes"
	"fmt"
	"github.com/alecthomas/chroma"
	"github.com/alecthomas/chroma/formatters/html"
	"github.com/alecthomas/chroma/lexers"
	"github.com/src-d/enry/v2"
	"github.com/valyala/fasthttp"
	"github.com/wI2L/jettison"
)

type HighlightResponse struct {
	Lang      string   `json:"lang"`
	Code      string   `json:"code"`
	Filenames []string `json:"filenames"`
}

// TODO: move line number stuff here from dogbin/app
var formatter = html.New(html.WithClasses(), html.TabWidth(2))

var supportedLangs []string

func highlightHandler(ctx *fasthttp.RequestCtx) {
	lang := string(ctx.Request.PostArgs().Peek("lang"))
	filename := string(ctx.Request.PostArgs().Peek("filename"))
	code := string(ctx.Request.PostArgs().Peek("code"))

	var lexer chroma.Lexer
	if lang != "" {
		lexer = lexers.Get(lang)
	}
	if lexer == nil && filename != "" {
		lexer = lexers.Get(enry.GetLanguage(filename, []byte(code)))
	}
	if lexer == nil && filename != "" {
		lexer = lexers.Match(filename)
	}
	var enryLang = ""
	// TODO: try to get less false positives for perl
	if lexer == nil {
		var safe = false
		// TODO: this is actually pretty bad, as it ends up returning random languages for everything below a certain minimum length
		enryLang, safe = enry.GetLanguageByClassifier([]byte(code), supportedLangs)
		if safe {
			lexer = lexers.Get(enryLang)
		}
	}
	if lexer == nil {
		lexer = lexers.Analyse(code)
		enryLang = ""
	}
	if lexer == nil {
		lexer = lexers.Get(enryLang)
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
	buf := bytes.Buffer{}
	err = formatter.Format(&buf, style, tokens)
	if err != nil {
		ctx.Error(fmt.Sprint(err), fasthttp.StatusInternalServerError)
		return
	}

	if enryLang == "" {
		enryLang, _ = enry.GetLanguageByAlias(lexer.Config().Name)
	}
	var filenames []string
	if enryLang != "" {
		filenames = enry.GetLanguageExtensions(enryLang)
	}
	filenames = append(filenames, lexer.Config().Filenames...)
	json, err := jettison.Marshal(HighlightResponse{
		Lang:      lexer.Config().Name,
		Code:      buf.String(),
		Filenames: filenames,
	})
	if err != nil {
		ctx.Error(fmt.Sprint(err), fasthttp.StatusInternalServerError)
		return
	}
	ctx.SetContentType("application/json")
	ctx.SetBody(json)
}

func main() {
	fmt.Println("dogbin - highlighter (v1.something)")
	fmt.Println("~ Collecting supported languages...")
	lexers.Register(Log)
	for _, l := range lexers.Registry.Lexers {
		config := l.Config()
		lang, ok := enry.GetLanguageByAlias(config.Name)
		if ok {
			supportedLangs = append(supportedLangs, lang)
		} else {
			for _, a := range config.Aliases {
				lang, ok := enry.GetLanguageByAlias(a)
				if ok {
					supportedLangs = append(supportedLangs, lang)
				}
			}
		}
	}
	fmt.Println("~ Started listening on port 8080")
	if err := fasthttp.ListenAndServe(":8080", highlightHandler); err != nil {
		fmt.Println(err)
	}
}
