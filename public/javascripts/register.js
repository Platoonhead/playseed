function Element(targetId, type, validators, requiredLength, errorMsg) {
    this.targetId = targetId;
    this.type = type;
    this.validators = validators;
    this.requiredLength = requiredLength;
    this.errorMsg = errorMsg;
}

var filled = true;

const emailRegex = /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
const dateformat = /^(?:(?:(?:0?[13578]|1[02])(\/|-|\.)31)\1|(?:(?:0?[1,3-9]|1[0-2])(\/|-|\.)(?:29|30)\2))(?:(?:1[6-9]|[2-9]\d)?\d{2})$|^(?:0?2(\/|-|\.)29\3(?:(?:(?:1[6-9]|[2-9]\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:(?:0?[1-9])|(?:1[0-2]))(\/|-|\.)(?:0?[1-9]|1\d|2[0-8])\4(?:(?:1[6-9]|[2-9]\d)?\d{2})$/;

const firstNameField = new Element("firstName", "textBox", ["filledFieldsValidator"], null, "Please enter your first name.");
const lastNameField = new Element("lastName", "textBox", ["filledFieldsValidator"], null, "Please enter your last name.");
const emailField = new Element("emailGroup_email", "textBox", ["filledFieldsValidator", "emailValidator"], null, "Please enter a valid email address.");
const confirmedEmailField = new Element("emailGroup_confirmedEmail", "textBox", ["filledFieldsValidator", "emailValidator", "matchEmailValidator"], null, "Confirm email must be the same as email.");
const address1Field = new Element("address1", "textBox", ["filledFieldsValidator"], null, "Please enter your address.");
const postalCodeField = new Element("postalCode", "textBox", ["filledFieldsValidator", "lengthFieldValidator"], 5, "Please enter a valid zip code.");
const provinceField = new Element("province", "select", ["filledFieldsValidator", "customStateValidator"], null, "Please select your state.");
const phoneField = new Element("phone", "textBox", ["filledFieldsValidator", "filledFieldsValidator", "lengthFieldValidator"], 10, "Please enter a valid phone number.");
const dob_birthYearField = new Element("dob_birthYear", "select", ["filledFieldsValidator", "yearValidator", "dateValidator"], null, "Please select your birth year.");
const dob_birthMonthField = new Element("dob_birthMonth", "select", ["filledFieldsValidator", "monthValidator", "dateValidator"], null, "Please select your birth month.");
const dob_birthDayField = new Element("dob_birthDay", "select", ["filledFieldsValidator", "dayValidator", "dateValidator"], null, "Please select your birth date.");
const isAgreeField = new Element("isAgree", "checkBox", ["filledFieldsValidator"], null, "Please agree with the terms and conditions of this promotional offer.");
const captchaField = new Element("captcha", "recaptcha", ["filledFieldsValidator"], null, "Please check the captcha checkbox.");

const allFields = [firstNameField,
    lastNameField,
    emailField,
    confirmedEmailField,
    address1Field,
    postalCodeField,
    provinceField,
    phoneField,
    dob_birthYearField,
    dob_birthMonthField,
    dob_birthDayField,
    isAgreeField,
    captchaField];

const checkForValidEmail = [emailField, confirmedEmailField];
const matchEmail = [confirmedEmailField];
const checkForValidDateFields = [dob_birthYearField, dob_birthMonthField, dob_birthDayField];
const checkForValidLengthFields = [phoneField, postalCodeField];

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
    if(targetId === 'isAgree') {
        $('#checkbox-error').after(htmlFormatted(targetId, errorMsg));
    } else {
        $("#" + targetId).after(htmlFormatted(targetId, errorMsg));
    }
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
            case ("select"):
                if (!(domElement.val().trim())) {
                    setError(element.targetId, element.errorMsg);
                }
                break;
            case ("checkBox") :
                if (!domElement.is(':checked')) {
                    setError(element.targetId, element.errorMsg);
                }
                break;
            case ("recaptcha") :
                if (!grecaptcha.getResponse()) {
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
    matchEmailValidator(matchEmail);
}

var validity = true;
function emailValidator(elements) {
    validity = true;
    elements.forEach(function (element) {
        var email = $("#" + element.targetId).val();
        if (!emailRegex.test(email)) {
            setError(element.targetId, element.errorMsg);
            validity = false;
        }
    });
}

function matchEmailValidator(elements) {
    if (validity) {
        elements.forEach(function (element) {
            var email = $("#" + emailField.targetId).val();
            var confirmEmail = $("#" + element.targetId).val();
            if (email != confirmEmail) {
                setError(element.targetId, "Confirm email must be the same as email.");
            }
        });
    } else {
        emailValidator(elements);
    }
}

function lengthFieldValidator(elements) {
    elements.forEach(function (element) {
        var field = $("#" + element.targetId).val();
        if (field.length != element.requiredLength && element.requiredLength != null) {
            setError(element.targetId, element.errorMsg);
        }
    });
}

var validDateFragments = true;
function validateDate(elements) {
    validDateFragments = true;
    yearValidator([elements[0]]);
    monthValidator([elements[1]]);
    dayValidator([elements[2]]);
    if (validDateFragments) {
        dateValidator(null)
    }
}

function yearValidator(elements) {
    elements.forEach(function (element) {
        var year = $("#" + element.targetId).val();
        if (year < 1947 || year > 2000) {
            validDateFragments = false;
            setError(element.targetId, element.errorMsg);
        }
    });
}

function monthValidator(elements) {
    elements.forEach(function (element) {
        var month = $("#" + element.targetId).val();
        if (month < 1 || month > 12) {
            validDateFragments = false;
            setError(element.targetId, element.errorMsg);
        }
    });
}

function dayValidator(elements) {
    elements.forEach(function (element) {
        var day = $("#" + element.targetId).val();
        if (day < 1 || day > 31) {
            validDateFragments = false;
            setError(element.targetId, element.errorMsg);
        }
    });
}

function dateValidator(_) {
        var day = $("#" + dob_birthDayField.targetId).val();
        var month = $("#" + dob_birthMonthField.targetId).val();
        var year = $("#" + dob_birthYearField.targetId).val();

        var inputDate = month + '/' + day + '/' + year;

        if (day && month && year && !(inputDate.match(dateformat))) {
            setError(dob_birthDayField.targetId, dob_birthDayField.errorMsg);
            setError(dob_birthMonthField.targetId, dob_birthMonthField.errorMsg);
            setError(dob_birthYearField.targetId, dob_birthYearField.errorMsg);
        } else if (day && month && year && (inputDate.match(dateformat))) {
            $("#error-" + dob_birthDayField.targetId).remove();
            $("#error-" + dob_birthMonthField.targetId).remove();
            $("#error-" + dob_birthYearField.targetId).remove();
        }
}

function customStateValidator(elements) {
    elements.forEach(function (element) {
        const notPermittedState = ["MN", "NJ", "OR", "PA", "SD", "UT"];
        const customMessage = "Unfortunately, this program is not available in your state. " +
            "Thank you for your purchase and please visit zalexanderbrown.com to learn more about our wines.";
        var stateElementDOM = $("#" + element.targetId);
        if (notPermittedState.includes(stateElementDOM.val())) {
            setError(element.targetId, customMessage);
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
        lengthFieldValidator(checkForValidLengthFields);
        validateDate(checkForValidDateFields);
        customStateValidator([provinceField]);
        slideToFirstError();
        return filled;
    });
});
