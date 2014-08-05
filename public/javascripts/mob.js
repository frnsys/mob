$(function() {
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket = new WS(WSRoute)
    var userCount = 0
    var userColors = {}
    var colorGenerator = new RColor

    var sendMessage = function() {
        chatSocket.send(JSON.stringify(
            {text: $(".speak").val()}
        ))
        $(".speak").val('')
    }

    var receiveEvent = function(event) {
        var data = JSON.parse(event.data)

        // Handle errors
        if(data.error) {
            chatSocket.close()
            $(".js-on-error span").text(data.error)
            $(".js-on-error").show()
            return
        } else {
            $(".js-on-chat, .discourse, .meta").show()
        }

        // On the initialization message.
        if (data.kind === "initial") {
            // Update the members list
            $(".members").empty()
            $(data.members).each(function() {
                $('.members').append('<li data-username="' + this + '">' + this + '</li>')
            })
            userCount = data.members.length;
            $(".size").html(userCount.toString() + ' people')

        // On any other kind of message.
        } else {
            // Create the message element
            var el = $('<div class="message"><span class="username"></span><p></p></div>')
            $("span", el).text(data.user)
            $("p", el).text(data.message)
            $(el).addClass(data.kind)
            if(data.user == '@username') $(el).addClass('me')
            $('.messages').append(el)

            if (data.kind === "join") {
                userCount++
                $(".size").html(userCount.toString() + ' people')
                $('.members').append('<li data-username="' + data.user + '">' + data.user + '</li>')
            }

            else if (data.kind === "quit") {
                userCount--
                $(".size").html(userCount.toString() + ' people')
                $('.members [data-username="' + data.user + '"]').remove()
            }

            else if (data.kind === "talk") {
                var userColor = userColors[data.user]
                if (userColor === undefined) {
                    userColor = colorGenerator.get(true)
                    userColors[data.user] = userColor
                }
                $("span", el).css("color", userColor);
            }

            // Scroll to bottom.
            $('.chat-stage').stop().animate({scrollTop: $('.messages').height() }, 10);
        }
    }

    var handleReturnKey = function(e) {
        if(e.charCode == 13 || e.keyCode == 13) {
            e.preventDefault()
            sendMessage()
        }
    }

    $(".speak").keypress(handleReturnKey)

    chatSocket.onmessage = receiveEvent

})


