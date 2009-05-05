
function validateDate(value, PastOrFuture)
    {
      return validateDate2(value, PastOrFuture, 0);
    }
    
    
    function validateDateFormatShort(value)
    {
      var dateregex = /(0[1-9]|[12][0-9]|3[01]).(0[1-9]|1[012]).(19|20)\d\d/;
      
      if(value.length != 10 || !value.match(dateregex))
        return false;
      else
        return true;
    }
    function validateDateFormat(value)
    {
      var dateregex = /(0[1-9]|[12][0-9]|3[01]).(0[1-9]|1[012]).(19|20)\d\d (0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])/;
      
      if(value.length != 16 || !value.match(dateregex))
        return false;
      else
        return true;
    }
    
    
function validateDate2(value, PastOrFuture, add_a_minute)
    {
    
      var dateregex = /(0[1-9]|[12][0-9]|3[01]).(0[1-9]|1[012]).(19|20)\d\d (0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])/;
      
      if(!value.match(dateregex))
      {
          return false;
      } else
      {
          //since we know the format now, we can use a series of substr's to extract the different parts
          //and create a date object, which can then be compared to now.
          
          var mainparts = value.split(' ');
          var time = mainparts[1];
          var date = mainparts[0];
          
          var timeparts = time.split(':');
          var hour = parseInt(timeparts[0], 10);
          var minute = parseInt(timeparts[1], 10);
          
          if(add_a_minute == 1)
          {
            if(minute != 0)
            {
              minute = minute - 1;              
            } else
            {
              hour = hour - 1;
              minute = 59;
            }
          }
          
          var dateparts = date.split('.');
          var year = parseInt(dateparts[2], 10);
          var month = parseInt(dateparts[1], 10);
          var day = parseInt(dateparts[0], 10);
          
          var dNow = new Date();
          //it seems this constructor requries months to start at 0-11, where as it is entered as 1-12
          var dEntered = new Date(year, month-1, day, hour, minute, 59, 999);
          
          if(PastOrFuture == 0) {
              if(dEntered.getTime() > dNow.getTime()) {
                  return false;
              }
          }else {
              if(dEntered.getTime() < dNow.getTime()) {
                 return false;
              }
          }
          return true;
      } 
    }
    
    
    function validateDateShort(value, PastOrFuture)
    {
    
      var dateregex = /(0[1-9]|[12][0-9]|3[01]).(0[1-9]|1[012]).(19|20)\d\d/;
      
      if(!value.match(dateregex))
      {
          return false;
      } else
      {
          //since we know the format now, we can use a series of substr's to extract the differnt parts
          //and create a date object, which can then be compared to now.
          
          var dateparts = value.split('.');
          var year = parseInt(dateparts[2], 10);
          var month = parseInt(dateparts[1], 10);
          var day = parseInt(dateparts[0], 10);
          
          var dNow = new Date();
          //it seems this constructor requries months to start at 0-11, where as it is entered as 1-12
          
          if(PastOrFuture == 0) {
            // must be in past
              var dEntered = new Date(year, month-1, day, 0, 0, 0, 0);
              if(dEntered.getTime() > dNow.getTime()) {
                  return false;
              }
          }else {
              var dEntered = new Date(year, month-1, day, 23, 59, 59, 999);
              if(dEntered.getTime() < dNow.getTime()) {
                 return false;
              }
          }
          return true;
      } 
    }
    
    
    function toDate(value, rounding) {
      var dateparts = value.split('.');
      var year = parseInt(dateparts[2], 10);
      var month = parseInt(dateparts[1], 10);
      var day = parseInt(dateparts[0], 10);
      if(rounding == 0) {
        return new Date(year, month-1, day, 0, 0, 0, 0);
      } else {
        return new Date(year, month-1, day, 23, 59, 59, 999);
      }
    }