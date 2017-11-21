$(document).ready(function () {

    $body = $("body");

    var receiptMessage = "";
    var imageSizeAlert = "";
    var errorMessage = "";
    var duplicateReceipt = "";
    var receiptSuccess = "";
    var success = "";

    success = "Congratulations";
    receiptMessage = "You have already submitted a receipt today, please try again tomorrow.";
    imageSizeAlert = "Please select your image by clicking on the box below.";
    errorMessage = "Something went wrong, please try again.";
    duplicateReceipt = "It appears that this receipt has been submitted before. Please submit a new receipt.";
    receiptSuccess = "Thank you! Your submissions has been accepted. Please allow 24-48 hours for us to validate your submission";


    $('#save-canvas-test').on('click', function () {
        if ($('#check-can-pull').html() == "yes") {
            saveCanvasTest();
        } else {
            alert(imageSizeAlert);
        }
    });

    $('#receipt-upload').on('click', function () {
        if (swal == 'undefined') {
            alert(receiptMessage);
            window.location.replace("/register");
        } else {
            swal("", receiptMessage, "warning");
        }
    });

    $('#upload-more-trick').on('click', function () {
        upladMoreReceipt();
    });

    var saveCanvasTest = function () {
        $.blockUI({
            css: {
                border: 'none',
                padding: '15px',
                backgroundColor: '#000',
                '-webkit-border-radius': '10px',
                '-moz-border-radius': '10px',
                opacity: .5,
                color: '#fff'
            }
        });

        var status = false;
        var encString1 = document.getElementById('image-data-holder1').innerHTML;
        var userInfo = document.getElementById('user-info-json').innerHTML;
        var csrfToken = getCookie('CSRF-Token');

        var imageData = dataURItoBlob(encString1);
        var fd = new FormData();
        fd.append("enc", imageData);

        jsRoutes.controllers.Receipts.receiveReceipt().ajax(
            {
                type: "POST",
                processData: false,
                contentType: false,
                data: fd,
                beforeSend: function (request) {
                    return request.setRequestHeader('CSRF-Token', csrfToken);
                },
                success: function (data) {
                    $(".upload-msg").empty();
                    setTimeout($.unblockUI, 0);
                    if (data == "d") {
                        swal("", duplicateReceipt, "warning");
                    } else if (data == "s") {
                        swal({
                            title: success,
                            text: receiptSuccess,
                            type: "success",
                            confirmButtonClass: "btn-danger",
                            confirmButtonText: "OK",
                            allowOutsideClick: false
                        }).then(function () {
                            window.location.replace("/");
                        })
                    } else {
                        $(".upload-msg").append('<div class="row flashing-position"> <div class="alert alert-danger fade in">' +
                            '<button id="flashBtn" type="button" class="close btn-warning" data-dismiss="alert">&times;</button>' +
                            '<strong></strong>' + errorMessage + '</div></div>'
                        );
                        alertClose();
                    }
                },
                error: function (data) {
                    setTimeout($.unblockUI, 0);
                    $('#warn-msg').remove();
                    $(".upload-msg").empty();
                    $(".upload-msg").append('<div class="row flashing-position"> <div class="alert alert-danger fade in">' +
                        '<button id="flashBtn" type="button" class="close btn-warning" data-dismiss="alert">&times;</button>' +
                        '<strong></strong>' + errorMessage + '</div></div>'
                    );
                    alertClose();
                },
                complete: function (comp) {
                }
            });
        return status;
    };

    function alertClose() {
        $('#flashBtn').click(function () {
            $('.flashing-position').remove();
        });
    }

    alertClose();
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

    function dataURItoBlob(imageData) {
        var byteString = atob(imageData.split(',')[1]);
        var imageType = (imageData.split(',')[0]);
        var ab = new ArrayBuffer(byteString.length);
        var ia = new Uint8Array(ab);
        for (var i = 0; i < byteString.length; i++) {
            ia[i] = byteString.charCodeAt(i);
        }
        return new Blob([ab], {type: imageType});
    }

});
