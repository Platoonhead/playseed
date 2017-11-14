var shouldLoad;

Dropzone.options.dropzoneArea = {
    thumbnailWidth: 421,
    thumbnailHeight: 520,
    paramName: "file",
    maxFilesize: 5,
    maxFiles: 1,
    autoProcessQueue: false,
    createImageThumbnails: true,
    parallelUploads: 0,
    clickable: "#dropzone-area,.receipt-upload-area, .dz-preview, .dz-details",
    acceptedFiles: ".jpeg,.jpg,.png,.JPEG,.JPG,.PNG",
    dictDefaultMessage: " ",
    previewTemplate: "<div class=\"dz-preview dz-file-preview\"> <div class=\"dz-details\"><img id=\"image-src-modern\" data-dz-thumbnail /> </div> </div>",
    accept: function (file, done) {
        done();
    },
    init: function () {
        this.on("error", function (file) {
            if (!file.accepted){
                shouldLoad = false;
                $(".txtMsg").show();
            }
            $('#check-can-pull').html("no");
            alert("Your image cannot exceed 5MB in size and must be a PNG, GIF or JPG file");
            this.removeFile(file);
            $(".txtMsg").show();
        });
        this.on("addedfile", function (file, xhr) {
            // console.log("file--------"+file);
            shouldLoad = true;
            if (this.files[1] != null) {
                // console.log("next image with file======"+this.files[0]);
                this.removeFile(this.files[0]);
                $('#check-can-pull').html("yes");
                //console.log("next image======");
                var test = $('#check-can-pull').val();
            }

            if (this.files[1] != null && !this.files[1].type.match('image.*')) {
                shouldLoad = false;
                //console.log("Added file2----."+shouldLoad);
                this.removeFile(this.files[1]);
            }
            if (this.files[0] != null && !this.files[0].type.match('image.*')) {
                shouldLoad = false;
                //console.log("Added file3----."+shouldLoad);
                this.removeFile(this.files[0]);
                $('#check-can-pull1').html("no");
            }
            if (this.files[1] != null && (this.files[1].size / 1024 / 1024) > 5) {
                shouldLoad = false;
                //console.log("Added file4----."+shouldLoad);
                this.removeFile(this.files[1]);
            }
            if (this.files[0] != null && (this.files[0].size / 1024 / 1024) > 5) {
                shouldLoad = false;
                //console.log("Added file5----."+shouldLoad);
                this.removeFile(this.files[0]);
                $('#check-can-pull').html("no");
            }

            var fr;
            fr = new FileReader;
            fr.onload = function () {
                var img;
                img = new Image;
                img.onload = function () {
                    return;
                };
                var imageData = fr.result;
                //console.log("imagedata = "+imageData);
                if (shouldLoad) {
                    document.getElementById("image-data-holder1").innerHTML = "" + imageData;
                    $('#check-can-pull').html("yes");
                    $(".txtMsg").hide();
                }
                return img.src = fr.result;
            };
            return fr.readAsDataURL(file);
        });
    },
    complete: function (file) {
        console.log("hello")
        this.on("sending", function (file, xhr) {
            //console.log("this.files[0]------"+this.files[0])
            if (this.files[0] == null) {
                //console.log("complete null")
            }
        });

        if (!file.type.match('image.*')) {
            //console.log("Upload Image Only!");
            return false;
        }
        if ((file.size / 1024 / 1024) > 5) {
            //console.log("Image size should not exceed 5mb!");
            return false;
        }
    }
};
