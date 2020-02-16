var haste_document = function () {
  var path = window.location.pathname;
  if (path !== '/' && path.indexOf('/', 1) === -1) { 
    var parts = path.substring(1, path.length).split('.',2)
    this.key = parts[0];
  } 
};

// Save this document to the server and lock it here
haste_document.prototype.save = function (data, callback) {
  $.ajax('/documents?frontend=true', {
    type: 'post',
    data: data,
    dataType: 'json',
    contentType: 'application/json; charset=utf-8',
    success: function (res) {
      callback(null, {
        key: res.key,
        isUrl: res.isUrl
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
  this.$customSlug = $('input[name=\'slug\']')
  this.configureShortcuts();
  this.configureButtons();
};

haste.prototype.fallbackCopyTextToClipboard = function(text) {
  var textArea = document.createElement("textarea");
  textArea.value = text;
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();

  try {
    var successful = document.execCommand('copy');
    var msg = successful ? 'Successfully' : 'Unsuccessful';
    var cls = successful ? 'info' : error;
    this.showMessage(msg + ' copied URL to the clipboard', cls)
  } catch (err) {
    console.error('Fallback: Oops, unable to copy', err);
  }

  document.body.removeChild(textArea);
}

haste.prototype.copyTextToClipboard = function(text) {
  if (!navigator.clipboard) {
    this.fallbackCopyTextToClipboard(text);
    return;
  }
  _this = this;
  navigator.clipboard.writeText(text).then(function() {
    _this.showMessage('Successfully copied URL to the clipboard')
  }, function(err) {
    console.error('Async: Could not copy text: ', err);
  });
}

// Show a message box
haste.prototype.showMessage = function (msg, cls) {
  var msgBox = $('<li class="' + (cls || 'info') + '">' + msg + '</li>');
  $('#messages').append(msgBox);
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

// Edit the current document
haste.prototype.editDocument = function () {
  if(this.doc.key){
    window.location.replace('/e/' + this.doc.key)
  }
};

// Lock the current document
haste.prototype.lockDocument = function () {
  var _this = this;
  this.doc.save(JSON.stringify({'content': this.$textarea.val(), 'slug': this.$customSlug.val()}), function (err, ret) {
    if (err) {
      _this.showMessage(err.message, 'error');
    } else if (ret) {
      if(ret.isUrl === true){
        window.location.replace('/v/' + ret.key);
      } else {
        window.location.replace('/' + ret.key);
      }
    }
  });
};

haste.prototype.configureButtons = function () {
  var _this = this;
  this.buttons = [{
    $where: $('.action.save'),
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
    $where: $('.action.edit'),
    shortcutDescription: 'control + e',
    shortcut: function (evt) {
      return evt.ctrlKey && (evt.keyCode === 69);
    },
    action: function () {
      _this.editDocument();
    }
  },
  {
    $where: $('.action.new'),
    shortcutDescription: 'control + n',
    shortcut: function (evt) {
      return evt.ctrlKey && evt.keyCode === 78;
    },
    action: function () {
      _this.newDocument();
    }
  },
  {
    $where: $('.action.raw'),
    shortcutDescription: 'control + shift + r',
    shortcut: function (evt) {
      return evt.ctrlKey && evt.shiftKey && evt.keyCode === 82;
    },
    action: function () {
      window.location.href = '/raw/' + _this.doc.key;
    }
  },
    {
      $where: $('.action.profile'),
      action: function () {
        window.location.href = '/me';
      }
    },
  {
    $where: $('.btn.copy_url'),
    action: function () {
      _this.copyTextToClipboard(window.location.origin + $('#url_display').attr("href"))
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
