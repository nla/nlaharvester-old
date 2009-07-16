var Harvester = {};

Harvester.tooltipURL = 'tooltip-example.html'; // THE id OF THE .jTip IS PASSED IN THROUGH THE QUERYSTRING: tooltip-example.html?id=note1

$(document).ready(function(){
	Harvester.iniCollapsible();
	Harvester.iniAddAnother();
	Harvester.iniPopUp();
	Harvester.iniDisabler();
	JT_init(); // TOOLTIPS
});

// CLUMSY: SETS UP FORM DISABLERS
Harvester.iniDisabler = function() {
	Harvester.toggleDisabler();
	Harvester.toggleGreyer();
	
	for (i=8; i>=1; i--) {
		$('.broadcast'+i).each(function(){
			// BUG IE7: YOU HAVE TO CLICK THE INPUT TWICE TO FIRE THE change EVENT
			$(this).click(function(){
				Harvester.toggleDisabler();
			});
		});
	}
	for (i=8; i>=1; i--) {
		$('.greyer'+i).each(function(){
			// BUG IE7: YOU HAVE TO CLICK THE INPUT TWICE TO FIRE THE change EVENT
			$(this).click(function(){
				Harvester.toggleGreyer();
			});
		});
	}
	$()
}

Harvester.toggleGreyer = function() {
	for (i=8; i>=1; i--) {
		$('.greyer'+i).each(function(){
			if ((!this.checked && this.type=='radio') || (this.checked && this.type=='checkbox')) {
				Harvester.toggleGrey(this, i, false);
			}
		});
	}
	// DISABLED STATE TAKES PRECEDENCE
	for (i=8; i>=1; i--) {
		$('.greyer'+i).each(function(){
			if ((this.checked && this.type=='radio') || (!this.checked && this.type=='checkbox')) {
				Harvester.toggleGrey(this, i, true);
			}
		});
	}
}

Harvester.toggleGrey = function(broadcaster, n, disable) {
	if (disable) {
		$('input.greyee'+i).addClass('disabled');
		$("input.greyee"+i).attr('disabled', 'disabled');
		$("input.greyee" + i + "[type='checkbox']").removeAttr('checked');
		//$('input.greyother'+n).addClass('active');
		//$('dt.listen'+i+' input, dd.listen'+i+' input, span.listen'+i+' input, .listen'+i+' dd input, .listen'+i+' dd select').attr('disabled', 'disabled');
		//$(broadcaster).parents('.option').addClass('active');
	} else {
		$('input.greyee'+i).removeClass('disabled');
		$("input.greyee"+i).removeAttr('disabled');		
		//$('input.greyother'+n).removeClass('active');
		//$('dt.listen'+i+' input, dd.listen'+i+', span.listen'+i+' input, .listen'+i+' dd input, .listen'+i+' dd select').attr('disabled', '');
		//$(broadcaster).parents('.option').removeClass('active');
	}
}


Harvester.toggleDisabler = function() {
	// LIMITED TO 8 BROADCASTERS/LISTENERS: broadcaster1 to broadcaster8
	for (i=8; i>=1; i--) {
		$('.broadcast'+i).each(function(){
			if ((!this.checked && this.type=='radio') || (this.checked && this.type=='checkbox')) {
				Harvester.toggleDisabled(this, i, false);
			}
		});
	}
	// DISABLED STATE TAKES PRECEDENCE
	for (i=8; i>=1; i--) {
		$('.broadcast'+i).each(function(){
			if ((this.checked && this.type=='radio') || (!this.checked && this.type=='checkbox')) {
				Harvester.toggleDisabled(this, i, true);
			}
		});
	}
}

Harvester.toggleDisabled = function(broadcaster, n, disable) {
	if (disable) {
		$('dt.listen'+i+', dd.listen'+i+', span.listen'+i+', .listen'+i+' dd').addClass('disabled');
		$('dt.listen'+i+' input, dd.listen'+i+' input, span.listen'+i+' input, .listen'+i+' dd input, .listen'+i+' dd select').attr('disabled', 'disabled');
		$(broadcaster).parents('.option').addClass('active');
	} else {
		$('dt.listen'+i+', dd.listen'+i+', span.listen'+i+', .listen'+i+' dd').removeClass('disabled');
		$('dt.listen'+i+' input, dd.listen'+i+', span.listen'+i+' input, .listen'+i+' dd input, .listen'+i+' dd select').attr('disabled', '');
		$(broadcaster).parents('.option').removeClass('active');
	}
}

// SETS UP POPUP BUTTONS
Harvester.iniPopUp = function() {
	Harvester.showPopUp(false);
	$('a.popup').click(function(){
		Harvester.showPopUp(true);
		return false;
	});
	$('#popup .cancel').click(function() {
		Harvester.showPopUp(false);
	});
	$('a.popup2').click(function(){
		Harvester.showPopUp2(true);
		return false;
	});
	$('#popup2 .cancel').click(function() {
		Harvester.showPopUp2(false);
	});
	$('a.popup3').click(function(){
		Harvester.showPopUp3(true);
		return false;
	});
	$('#popup3 .cancel').click(function() {
		Harvester.showPopUp3(false);
	});		
}

// DISPLAYS THE PRE-GENERATED POPUP
Harvester.showPopUp = function(show) {
	$('#popup').css('display', show ? 'block' : 'none');
}
Harvester.showPopUp2 = function(show) {
	$('#popup2').css('display', show ? 'block' : 'none');
}
Harvester.showPopUp3 = function(show) {
	$('#popup3').css('display', show ? 'block' : 'none');
}

// ADD ANOTHER ROW/FIELD - <a> ONLY
Harvester.iniAddAnother = function() {
	$('a.add').click(function(){
		Harvester.addAnother($(this));
		return false;
	});
	$('.clone').each(function(){
		Harvester.iniRemoveButton(this);
	});
	$('a.add2').click(function(){
		Harvester.addAnother2($(this));
		return false;
	});
	$('.clone2').each(function(){
		Harvester.iniRemoveButton2(this);
	});
}

// ADDS ANOTHER ROW
Harvester.addAnother = function(button) {
	var last = $('.clone', button.parents('.cloner')).filter(':last'); // TRAVERSE OUT OF .buttons BACK AS FAR AS .cloner
	if (last.is('.hidden')) {
		last.removeClass('hidden'); // IF THE FIRST PLACEHOLDER IS HIDDEN (USUALLY TO PRESERVE LAYOUT)
		// FIX IE7: CAN'T REMOVE EMPTY FIELDS
		$('input, select', last).each(function() {
			$(this).attr('disabled', '');
		});
	} else {
		// CLONES LAST .clone
		checked = $(".dontcopy", last).attr('checked');
		var clone = last.clone().addClass('clone').insertAfter(last);
		$(".dontcopy", clone).remove();
		$(".dontcopy", last).attr('checked', checked);
		
		var nameinfield = $('input:not(.dontcopy), select', last).filter(':first').attr('name');
		//num = parseInt(nameinfield.replace(/[^0-9]*/,''))+1;
		//adefazio: the code that was here only worked for names without any other numbers in them, so i've fixed that
		positionofendnum = nameinfield.search(/\d+$/);
		mynumber = parseInt(nameinfield.substr(positionofendnum))+1;
		$('input, select, a.picker', clone).each(function() {
			name = this.name.replace(/\d+$/,'');
			$(this).attr({'name':name+mynumber})
				.filter(':radio, :checkbox').attr({'checked':''}).end()
				.filter(':text').filter(':not(.leavealone)').attr({'value':''});
		});
		$('select', clone).each(function() {
			this.selectedIndex='0'; // DEFAULT SELECTED INDEX
		});
		clone.removeClass('first'); // FOR TIMES/DATES IN THE SCHEDULE
	}
	Harvester.iniRemoveButton(clone);
	// ADDS "ANOTHER" TO BUTTON IF NOT PRESENT
	var html = button.html();
	if (html.toLowerCase().indexOf('another') < 0) {
		html = html.replace(/Add a/, 'Add Another');
		button.html(html);
	}
}

// REMOVE BUTTON: DELETES OR HIDES ROW
Harvester.iniRemoveButton = function(clone) {
	$('.delete', clone).click(function(){
		var clone = $(this).parents('.clone');
		var n = clone.siblings('.clone');
		//n = n.length;
		if (n.length > 0) {
			$('.mappingtd',clone.next()).append($('.dontcopy', clone).clone());			
			clone.remove();
		} else {
			// HIDES IF LAST ROW
			$('input:not(.dontcopy), select', clone).each(function() {
				$(this).attr('disabled', 'disabled') // SO YOU CAN'T SUBMIT NO ROW
					.filter(':text').attr('value', '').end()
					.filter(':checkbox').attr('checked', '').end()
			});
			$('select', clone).each(function() {
				this.selectedIndex='0';
			});
			
			//special case for convert value
			$('.dontcopy', clone).each(function() {
				$("#mappingusedradio").attr('checked', 'checked');
			});
			
			clone.addClass('hidden');
		}
		return false;
	});
	
}






Harvester.addAnother2 = function(button) {
	
	// ADDS "ANOTHER" TO BUTTON IF NOT PRESENT
	var html = button.html();
	if (html.toLowerCase().indexOf('another') < 0) {
		html = html.replace(/Add a/, 'Add Another');
		button.html(html);
	}
	
	var last = $('.clone2', button.parents('.cloner')).filter(':last'); // TRAVERSE OUT OF .buttons BACK AS FAR AS .cloner
	if (last.is('.hidden')) {
		last.removeClass('hidden'); // IF THE FIRST PLACEHOLDER IS HIDDEN (USUALLY TO PRESERVE LAYOUT)
		// FIX IE7: CAN'T REMOVE EMPTY FIELDS
		$('input, select', last).each(function() {
			$(this).attr('disabled', '');
		});
	} else {
		// CLONES LAST .clone
		var clone = last.clone().addClass('clone2').insertAfter(last);
		var num = $('input, select', last).filter(':first').attr('name');
		num = parseInt(num.replace(/[^0-9]*/,''))+1;
		$('input, select', clone).each(function() {
			name = this.name.replace(/[0-9]+/,'');
			$(this).attr({'name':name+num})
				.filter(':radio, :checkbox').attr({'checked':''}).end()
				.filter(':text').filter(':not(.leavealone)').attr({'value':''});
		});
		$('select', clone).each(function() {
			this.selectedIndex='0'; // DEFAULT SELECTED INDEX
		});
		clone.removeClass('first'); // FOR TIMES/DATES IN THE SCHEDULE
	}
	Harvester.iniRemoveButton2(clone);
}

// REMOVE BUTTON: DELETES OR HIDES ROW
Harvester.iniRemoveButton2 = function(clone) {
	$('.delete2', clone).click(function(){
		var clone = $(this).parents('.clone2');
		var n = clone.siblings('.clone2');
		n = n.length;
		if (n>0) {
			clone.remove();
		} else {
			// HIDES IF LAST ROW
			$('input, select', clone).each(function() {
				$(this).attr('disabled', 'disabled') // SO YOU CAN'T SUBMIT NO ROW
					.filter(':text').attr('value', '').end()
					.filter(':checkbox').attr('checked', '').end()
			});
			$('select', clone).each(function() {
				this.selectedIndex='0';
			});
			clone.addClass('hidden');
		}
		return false;
	});
}








// MAKES ALL ADVANCED PANES COLLAPSIBLE
Harvester.iniCollapsible = function() {
	var html = $('.advanced h4').click(function(){
		if (!$(this).animating) {
			$(this).animating = true;
			Harvester.toggleCollapse($(this));
		}
		return false;
	}).next().hide();
}

// PERFORMS THE COLLAPSE
Harvester.toggleCollapse = function(h4) {
	var parent = h4.parent();
	var content = h4.next();
	if (parent.is('.collapsed')) {
		content.hide();
		parent.removeClass('collapsed');
		content.slideDown('medium', function(){
			$(this).css('height', 'auto');
			this.animating = false;
		});
	} else {
		content.slideUp('medium', function() {
			$(this.parentNode).addClass('collapsed');
			this.animating = false;
		});
	}
}

///////////////////////////////////////////////////////////////////////////////////////////////////
// MODIFIED FOR USE IN HARVESTER APPLICATION

/*
 * JTip
 * By Cody Lindley (http://www.codylindley.com)
 * Under an Attribution, Share Alike License
 * JTip is built on top of the very light weight jquery library.
 */

function JT_init(){
	   $("a.jTip")
	   .hover(function(){JT_show(Harvester.tooltipURL+'?id='+this.id,this.id,this.name)},function(){$('#JT').remove()})
           //.click(function(){return false});	   
}

function JT_show(url,linkId,title){
	if(title == false)title="&nbsp;";
	var de = document.documentElement;
	var w = self.innerWidth || (de&&de.clientWidth) || document.body.clientWidth;
	var hasArea = w - getAbsoluteLeft(linkId);
	var clickElementy = getAbsoluteTop(linkId) - 3; //set y position
	var queryString = url.replace(/^[^\?]+\??/,'');
	var params = parseQuery( queryString );
	if(params['width'] === undefined){params['width'] = 250};
	if(params['link'] !== undefined){
	$('#' + linkId).bind('click',function(){window.location = params['link']});
	$('#' + linkId).css('cursor','pointer');
	}
	if(hasArea>((params['width']*1)+75)){
		$("body").append("<div id='JT' style='width:"+params['width']*1+"px'><div id='JT_arrow_left'></div><div id='JT_close_left'>"+title+"</div><div id='JT_copy'><div class='JT_loader'><div></div></div>");//right side
		var arrowOffset = getElementWidth(linkId) + 11;
		var clickElementx = getAbsoluteLeft(linkId) + arrowOffset; //set x position
	}else{
		$("body").append("<div id='JT' style='width:"+params['width']*1+"px'><div id='JT_arrow_right' style='left:"+((params['width']*1)+1)+"px'></div><div id='JT_close_right'>"+title+"</div><div id='JT_copy'><div class='JT_loader'><div></div></div>");//left side
		var clickElementx = getAbsoluteLeft(linkId) - ((params['width']*1) + 15); //set x position
	}
	
	$('#JT').css({left: clickElementx+"px", top: clickElementy+"px"});
	$('#JT').show();
	$('#JT_copy').load(url);

}

function getElementWidth(objectId) {
	x = document.getElementById(objectId);
	return x.offsetWidth;
}

function getAbsoluteLeft(objectId) {
	// Get an object left position from the upper left viewport corner
	o = document.getElementById(objectId)
	oLeft = o.offsetLeft            // Get left position from the parent object
	while(o.offsetParent!=null) {   // Parse the parent hierarchy up to the document element
		oParent = o.offsetParent    // Get parent object reference
		oLeft += oParent.offsetLeft // Add parent left position
		o = oParent
	}
	return oLeft
}

function getAbsoluteTop(objectId) {
	// Get an object top position from the upper left viewport corner
	o = document.getElementById(objectId)
	oTop = o.offsetTop            // Get top position from the parent object
	while(o.offsetParent!=null) { // Parse the parent hierarchy up to the document element
		oParent = o.offsetParent  // Get parent object reference
		oTop += oParent.offsetTop // Add parent top position
		o = oParent
	}
	return oTop
}

function parseQuery ( query ) {
   var Params = new Object ();
   if ( ! query ) return Params; // return empty object
   var Pairs = query.split(/[;&]/);
   for ( var i = 0; i < Pairs.length; i++ ) {
      var KeyVal = Pairs[i].split('=');
      if ( ! KeyVal || KeyVal.length != 2 ) continue;
      var key = unescape( KeyVal[0] );
      var val = unescape( KeyVal[1] );
      val = val.replace(/\+/g, ' ');
      Params[key] = val;
   }
   return Params;
}

function blockEvents(evt) {
	if(evt.target){
		evt.preventDefault();
	}else{
		evt.returnValue = false;
	}
}
