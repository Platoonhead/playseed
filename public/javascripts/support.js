function Element(targetId, type, validators, errorMsg) {
    this.targetId = targetId;
    this.type = type;
    this.validators = validators;
    this.errorMsg = errorMsg;
}

var filled = true;

const emailRegex = /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

const nameField = new Element("name", "textBox", ["filledFieldsValidator"], "Please enter your name.");
const emailField = new Element("email", "textBox", ["filledFieldsValidator", "emailValidator"], "Please enter a valid email address.");
const messageField = new Element("message", "textBox", ["filledFieldsValidator"], "The message is required.");

const allFields = [nameField, emailField, messageField];
const checkForValidEmail = [emailField];

function htmlFormatted(id, message) {
    return '<span id="error-' + id + '" class="help-block align-error custom-error-locator">' + message + '</span>'
}

function slideToFirstError() {
    var element = document.getElementsByClassName("custom-error-locator")[0];
    $('html, body').animate({
        scrollTop: $(element).offset().top - 150
    }, 500);
}

function setError(targetId, errorMsg) {
    filled = false;
    $("#error-" + targetId).remove();
    $("#" + targetId).after(htmlFormatted(targetId, errorMsg));
}

function filledFieldsValidator(elements) {
    elements.forEach(function (element) {
        var domElement = $("#" + element.targetId);

        switch (element.type) {
            case ("textBox"):
                if (!(domElement.val().trim())) {
                    setError(element.targetId, element.errorMsg);
                }
                break;
            default :
                console.log("Validation not Supported");
        }
    });
}

function validateEmails(elements) {
    emailValidator(elements);
}


function emailValidator(elements) {
    elements.forEach(function (element) {
        var email = $("#" + element.targetId).val();
        if (!emailRegex.test(email)) {
            setError(element.targetId, element.errorMsg);
        }
    });
}

/**
 * Binds the validators provided to the respective element instances to onBlur call
 */
$(document).ready(function () {
    allFields.forEach(function (element) {
        $("#" + element.targetId).on('blur', function () {
            var validators = element.validators;
            $("#error-" + element.targetId).remove();
            validators.forEach(function (validator) {
                window[validator]([element]);
            });
        });
    });

    /**
     * Entry Point
     */
    $("#my-form").submit(function () {
        filled = true;
        filledFieldsValidator(allFields);
        validateEmails(checkForValidEmail);
        slideToFirstError();
        return filled;
    });
});
