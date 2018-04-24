// Map of common extensions
// Note: this list does not need to include anything that IS its extension,
// due to the behavior of lookupTypeByExtension and lookupExtensionByType
// Note: optimized for lookupTypeByExtension
extensionMap = {
  ruby: 'rb',
  python: 'py',
  perl: 'pl',
  php: 'php',
  scala: 'scala',
  go: 'go',
  xml: 'xml',
  xml: 'html',
  xml: 'htm',
  css: 'css',
  javascript: 'js',
  vbscript: 'vbs',
  lua: 'lua',
  delphi: 'pas',
  java: 'java',
  cpp: 'cpp',
  cpp: 'cc',
  objectivec: 'm',
  vala: 'vala',
  sql: 'sql',
  smalltalk: 'sm',
  lisp: 'lisp',
  ini: 'ini',
  diff: 'diff',
  bash: 'bash',
  bash: 'sh',
  tex: 'tex',
  erlang: 'erl',
  haskell: 'hs',
  markdown: 'md',
  nohighlight: 'txt',
  coffee: 'coffee',
  javascript: 'json',
  swift: 'swift'
};

// Look up the extension preferred for a type
// If not found, return the type itself - which we'll place as the extension
lookupExtensionByType = function (type) {
  return extensionMap[type] || type;
};

var haste_document = function () {
  var path = window.location.pathname;
  if (path !== '/') { 
    var parts = path.substring(1, path.length).split('.',2)
    this.key = parts[0];
    hljs.initHighlighting()
    if(parts.length === 1){
      lang = $('code.hljs')[0].classList[1]
      extension = lookupExtensionByType(lang)
      window.history.pushState(null, document.title, path + '.' + extension);
    }
  } 
};

// Save this document to the server and lock it here
haste_document.prototype.save = function (data, callback) {
  $.ajax('/documents', {
    type: 'post',
    data: data,
    dataType: 'json',
    contentType: 'application/json; charset=utf-8',
    success: function (res) {
      callback(null, {
        key: res.key
      });
    },
    error: function (res) {
      try {
        callback($.parseJSON(res.responseText));
      } catch (e) {
        callback({
          message: 'Something went wrong!'
        });
      }
    }
  });
};

///// represents the paste application

var haste = function () {
  this.doc = new haste_document();
  this.$textarea = $('textarea');
  this.configureShortcuts();
  this.configureButtons();
};

// Show a message box
haste.prototype.showMessage = function (msg, cls) {
  var msgBox = $('<li class="' + (cls || 'info') + '">' + msg + '</li>');
  $('#messages').prepend(msgBox);
  setTimeout(function () {
    msgBox.slideUp('fast', function () {
      $(this).remove();
    });
  }, 3000);
};

// Remove the current document (if there is one)
// and set up for a new one
haste.prototype.newDocument = function () {
  if (this.doc.key) {
    window.location.replace('/')
  } else {
    this.$textarea.val('')
  }
};

// Duplicate the current document
haste.prototype.duplicateDocument = function () {
  if(this.doc.key){
    window.location.replace('/?duplicate=' + this.doc.key)
  }
};

// Lock the current document
haste.prototype.lockDocument = function () {
  var _this = this;
  this.doc.save(this.$textarea.val(), function (err, ret) {
    if (err) {
      _this.showMessage(err.message, 'error');
    } else if (ret) {
      window.location.replace('/' + ret.key);
    }
  });
};

haste.prototype.configureButtons = function () {
  var _this = this;
  this.buttons = [{
    $where: $('#box2 .save'),
    label: 'Save',
    shortcutDescription: 'control + s',
    shortcut: function (evt) {
      return evt.ctrlKey && (evt.keyCode === 83);
    },
    action: function () {
      if (_this.$textarea.val().replace(/^\s+|\s+$/g, '') !== '') {
        _this.lockDocument();
      }
    }
  },
  {
    $where: $('#box2 .new'),
    label: 'New',
    shortcut: function (evt) {
      return evt.ctrlKey && evt.keyCode === 78;
    },
    shortcutDescription: 'control + n',
    action: function () {
      _this.newDocument();
    }
  },
  {
    $where: $('#box2 .duplicate'),
    label: 'Duplicate & Edit',
    shortcut: function (evt) {
      return evt.ctrlKey && evt.keyCode === 68;
    },
    shortcutDescription: 'control + d',
    action: function () {
      _this.duplicateDocument();
    }
  },
  {
    $where: $('#box2 .raw'),
    label: 'Just Text',
    shortcut: function (evt) {
      return evt.ctrlKey && evt.shiftKey && evt.keyCode === 82;
    },
    shortcutDescription: 'control + shift + r',
    action: function () {
      window.location.href = '/raw/' + _this.doc.key;
    }
  },
  {
    $where: $('#box2 .twitter'),
    label: 'Twitter',
    shortcut: function (evt) {
      return evt.shiftKey && evt.ctrlKey && evt.keyCode == 84;
    },
    shortcutDescription: 'control + shift + t',
    action: function () {
      window.open('https://twitter.com/share?url=' + encodeURI(window.location.href));
    }
  }
  ];
  for (var i = 0; i < this.buttons.length; i++) {
    this.configureButton(this.buttons[i]);
  }
};

haste.prototype.configureButton = function (options) {
  // Handle the click action
  options.$where.click(function (evt) {
    evt.preventDefault();
    if ($(this).hasClass('enabled')) {
      options.action();
    }
  });
  // Show the label
  options.$where.mouseenter(function () {
    $('#box3 .label').text(options.label);
    $('#box3 .shortcut').text(options.shortcutDescription || '');
    $('#box3').show();
    $(this).append($('#pointer').remove().show());
  });
  // Hide the label
  options.$where.mouseleave(function () {
    $('#box3').hide();
    $('#pointer').hide();
  });
};

// Configure keyboard shortcuts for the textarea
haste.prototype.configureShortcuts = function () {
  var _this = this;
  $(document.body).keydown(function (evt) {
    var button;
    for (var i = 0; i < _this.buttons.length; i++) {
      button = _this.buttons[i];
      if (button.shortcut && button.shortcut(evt)) {
        evt.preventDefault();
        button.action();
        return;
      }
    }
  });
};

///// Tab behavior in the textarea - 2 spaces per tab
$(function () {

  $('textarea').keydown(function (evt) {
    if (evt.keyCode === 9) {
      evt.preventDefault();
      var myValue = '  ';
      // http://stackoverflow.com/questions/946534/insert-text-into-textarea-with-jquery
      // For browsers like Internet Explorer
      if (document.selection) {
        this.focus();
        var sel = document.selection.createRange();
        sel.text = myValue;
        this.focus();
      }
      // Mozilla and Webkit
      else if (this.selectionStart || this.selectionStart == '0') {
        var startPos = this.selectionStart;
        var endPos = this.selectionEnd;
        var scrollTop = this.scrollTop;
        this.value = this.value.substring(0, startPos) + myValue +
          this.value.substring(endPos, this.value.length);
        this.focus();
        this.selectionStart = startPos + myValue.length;
        this.selectionEnd = startPos + myValue.length;
        this.scrollTop = scrollTop;
      } else {
        this.value += myValue;
        this.focus();
      }
    }
  });

});
