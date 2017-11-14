/*
$(document).ready(function () {
    var message = "";
    message = "if you are 18 years old, Click OK to go to the registration page otherwise click CANCEL";

    $("#register").click(confirmAge);
    $("#register-mobile").click(confirmAge);

    function confirmAge() {
        swal({
                title: "",
                text: message,
                type: "warning",
                showCancelButton: true,
                confirmButtonClass: "btn-danger",
                confirmButtonText: "OK",
                cancelButtonText: "CANCEL",
                closeOnConfirm: false,
                closeOnCancel: false
            },
            function (isConfirm) {
                if (isConfirm) {
                    window.location.replace("/register");
                } else {
                    window.location.replace("/");
                }
            });
    }

});
*/
