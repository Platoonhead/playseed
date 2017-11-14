$(document).ready(function () {

    $body = $("body");

    var message = "";
    var errorMessage = "";
    var cancelBtn = "";
    message = "It appears that you have not registered for this program yet. You will need to register in order to participate";
    errorMessage = "Please enter a valid email address.";
    cancelBtn = "cancel";

    $("#verifyUpload").click(uploadVerification);

    function validateEmail(email) {
        var re = /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        return re.test(email);
    }

    $("#upload_email").on('blur', function () {
        if (($("#upload_email")).val() === "" || validateEmail($("#upload_email").val().trim()) === false) {
            var emailErrorMessage = "<span class='help-block align-error'>" + errorMessage + "</span>";
            if ($("#upload_email").next().html() != "") {
                $("#upload_email").next().remove();
                $("#upload_email").after(emailErrorMessage);
            } else {
                $("#upload_email").parent().addClass('has-error');
                $("#upload_email").next().remove();
                $("#upload_email").after(emailErrorMessage);
            }
        } else {
            $("#upload_email").next().html("");
        }
    });

    function uploadVerification() {
        var csrfToken = getCookie('CSRF-Token');
        var email = $('#upload_email').val();

        var fd = new FormData();
        fd.append("email", email);

        jsRoutes.controllers.UploadVerification.uploadVerification().ajax(
            {
                type: "POST",
                processData: false,
                contentType: false,
                data: fd,
                beforeSend: function (request) {
                    return request.setRequestHeader('CSRF-Token', csrfToken);
                },
                success: function (data) {
                    if (data == "404") {
                        if (swal == 'undefined') {
                            alert(message);
                            window.location.replace("/register");
                        } else {
                            swal({
                                title: "",
                                text: message,
                                type: "warning",
                                showCancelButton: true,
                                confirmButtonClass: "btn-danger",
                                confirmButtonText: "OK",
                                cancelButtonText: cancelBtn,
                                allowOutsideClick: false
                            }).then(function () {
                                window.location.replace("/register");
                            }, function (dismiss) {
                                // dismiss can be 'overlay', 'cancel', 'close', 'esc', 'timer'
                                if (dismiss === 'cancel') {
                                    window.location.replace("/upload");
                                }
                            });
                        }
                    } else if (data == "200") {
                        window.location.replace("/upload");
                    } else if (data == "400") {
                        var emailErrorMessage = "<span class='help-block align-error'>" + errorMessage + "</span>";
                        $("#upload_email").after(emailErrorMessage);
                    }
                }

            })
    }

    function getCookie(name) {
        var cookieValue = null;
        if (document.cookie && document.cookie !== '') {
            var cookies = document.cookie.split(';');
            for (var i = 0; i < cookies.length; i++) {
                var cookie = jQuery.trim(cookies[i]);
                if (cookie.substring(0, name.length + 1) === (name + '=')) {
                    cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                    break;
                }
            }
        }
        return cookieValue;
    }

});
