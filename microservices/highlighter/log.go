package main

import (
	. "github.com/alecthomas/chroma"
	"regexp"
	"strings"
)

// Android log lexer.
var Log = MustNewLexer(
	&Config{
		Name:      "Log",
		Aliases:   []string{"log", "logcat"},
		Filenames: []string{"*.log"},
		MimeTypes: []string{},
	},
	Rules{
		"root": {
			{`^(--------- beginning of.+$)`, GenericHeading, nil},
			{`^([0-9]{2}-[0-9]{2})(.+$)`, ByGroups(LiteralDate, UsingSelf("base")), nil},
		},
		"base": {
			{`(\d{2})(:)(\d{2})(:)(\d{2})(.)(\d{3})`, ByGroups(LiteralDate, Operator, LiteralDate, Operator, LiteralDate, Operator, LiteralDate), nil},
			{`(\d+\s+\d+)`, NumberIntegerLong, nil},
			{`(\s[FE])(\s+.+?)(:)(.+$)`, ByGroups(GenericError, GenericError, Operator, UsingSelf("line")), nil},
			{`(\sW)(\s+.+?)(:)(.+$)`, ByGroups(NameFunction, NameFunction, Operator, UsingSelf("line")), nil},
			{`(\sI)(\s+.+?)(:)(.+$)`, ByGroups(Comment, Comment, Operator, UsingSelf("line")), nil},
			{`(\sV)(\s+.+?)(:)(.+$)`, ByGroups(Keyword, Keyword, Operator, UsingSelf("line")), nil},
			{`(\sD)(\s+.+?)(:)(.+$)`, ByGroups(GenericPrompt, GenericPrompt, Operator, UsingSelf("line")), nil},
		},
		"line": {
			{`\[\w*?FATAL\w*?\]`, GenericError, nil},
			{`\s+FATAL EXCEPTION:.+$`, GenericError, nil},
			{`\s+.. \d+ more\s*?$`, GenericError, nil},
			{`(\s+Process: )(.+?)(, PID: )(\d+)(.*?$)`, ByGroups(GenericError, NameConstant, GenericError, NumberInteger, GenericError), nil},
			{`\s*?(Caused By: |)[\w\.\$]+(Exception|Error|Throwable):?.*?$`, GenericError, nil},
			{`(\s+at )(.+?\()(.+?(?:\.java|\.kt):\d+|Native Method)(\).*?$)`, ByGroups(GenericError, NameClass, String, NameClass), nil},
			{`\[[A-Z_-]\]`, KeywordConstant, nil},
			{`.`, Text, nil},
		},
	},
).SetAnalyser(func(text string) float32 {
	if strings.Contains(text, "W ActivityManager: Slow operation:") {
		return 0.75
	}
	commonTagsRe := regexp.MustCompile(`[FEWIVD]\s+(PackageManager|SystemServer|SystemServiceManager|ActivityManager|system_server|AndroidRuntime)\s*:`)
	if commonTagsRe.MatchString(text) {
		return 0.7
	} else if strings.Contains(text, "OMXClient: IOmx service obtained") {
		return 0.6
	} else if strings.Contains(text, "--------- beginning of system") || strings.Contains(text, "--------- beginning of main") {
		return 0.5
	}
	return 0
})
