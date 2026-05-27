# StudyNotion Source Archive

This file contains the current source files from the backend and root config, grouped by path for easier porting to Spring Boot + React. It intentionally excludes dependency folders and binary assets.

## backend\config\cloudinary.js

```javascript
const cloudinary = require("cloudinary").v2;

exports.cloudinaryConnect = () => {
  try {
    cloudinary.config({
      cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
      api_key: process.env.CLOUDINARY_API_KEY,
      api_secret: process.env.CLOUDINARY_API_SECRET,
    });

    return cloudinary;
  } catch (error) {
    console.error("Cloudinary connection failed", error);
    throw error;
  }
};


```

## backend\config\database.js

```javascript
const mongoose = require('mongoose');
const dotenv = require('dotenv');
require("dotenv").config();

exports.connect = () =>{
    mongoose.connect(process.env.MONGODB_URL).then(()=>{
        console.log("Database connected successfully");
    })
    .catch((err)=>{
        console.log("Database connection failed", err);
        process.exit(1);
    });

}

```

## backend\config\razorpay.js

```javascript
const Razorpay = require("razorpay");

exports.instance = new Razorpay({
  key_id: process.env.RAZORPAY_KEY,
  key_secret: process.env.RAZORPAY_SECRET,
});


```

## backend\controllers\Auth.js

```javascript
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const otpGenerator = require("otp-generator");

const User = require("../models/User");
const OTP = require("../models/OTP");
const Profile = require("../models/Profile");

require("dotenv").config();

// Route expects: sendotp
exports.sendotp = async (req, res , next) => {
  try {
    const { email } = req.body;

    if (!email) {
      return res.status(400).json({
        success: false,
        message: "email is required.",
      });
    }

    const userExists = await User.findOne({ email });
    if (userExists) {
      return res.status(409).json({
        success: false,
        message: "User already exists.",
      });
    }

    let otp = otpGenerator.generate(6, {
      upperCaseAlphabets: false,
      lowerCaseAlphabets: false,
      specialChars: false,
    });

    // ensure uniqueness among active OTPs
    while (await OTP.findOne({ otp })) {
      otp = otpGenerator.generate(6, {
        upperCaseAlphabets: false,
        lowerCaseAlphabets: false,
        specialChars: false,
      });
    }

    await OTP.create({ email, otp });

    return res.status(200).json({
      success: true,
      message: "OTP sent successfully.",
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Failed to generate OTP.",
      error: error.message,
    });
  }
};

// aliases (some code uses different casing)
exports.sendOTP = exports.sendotp;

// Route expects: signup
exports.signup = async (req, res) => {
  try {
    const {
      firstName,
      lastName,
      email,
      password,
      confirmPassword,
      accountType = "Student",
      contactNumber = "",
      otp,
      gender = "Other",
      dateOfBirth = "2000-01-01",
    } = req.body;

    if (
      !firstName ||
      !lastName ||
      !email ||
      !password ||
      !confirmPassword ||
      !otp
    ) {
      return res.status(400).json({
        success: false,
        message: "All required fields must be provided.",
      });
    }

    if (password !== confirmPassword) {
      return res.status(400).json({
        success: false,
        message: "Password and confirmPassword do not match.",
      });
    }

    const existingUser = await User.findOne({ email });
    if (existingUser) {
      return res.status(409).json({
        success: false,
        message: "User already exists.",
      });
    }

    const recentOtp = await OTP.find({ email })
      .sort({ createdAt: -1 })
      .limit(1);

    if (!recentOtp.length) {
      return res.status(400).json({
        success: false,
        message: "OTP not found. Please request a new OTP.",
      });
    }

    if (String(otp) !== String(recentOtp[0].otp)) {
      return res.status(400).json({
        success: false,
        message: "Invalid OTP.",
      });
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    const profileDetails = await Profile.create({
      gender: gender || "Other",
      dateOfBirth: dateOfBirth ? new Date(dateOfBirth) : new Date("2000-01-01"),
      about: null,
      contactNumber: contactNumber || "",
    });

    const user = await User.create({
      firstName,
      lastName,
      email,
      contactNumber,
      accountType,
      password: hashedPassword,
      additionalDetails: profileDetails._id,
      image: `https://api.dicebear.com/9.x/initials/svg?seed=${firstName}%20${lastName}`,
    });

    user.password = undefined;

    return res.status(201).json({
      success: true,
      message: "User registered successfully.",
      user,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "User registration failed.",
      error: error.message,
    });
  }
};

exports.signUp = exports.signup;

exports.login = async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({
        success: false,
        message: "email and password are required.",
      });
    }

    const user = await User.findOne({ email }).populate("additionalDetails");
    if (!user) {
      return res.status(401).json({
        success: false,
        message: "User is not registered. Please signup first.",
      });
    }

    const ok = await bcrypt.compare(password, user.password);
    if (!ok) {
      return res.status(401).json({
        success: false,
        message: "Password is incorrect.",
      });
    }

    const payload = {
      email: user.email,
      id: user._id,
      accountType: user.accountType,
    };

    const token = jwt.sign(payload, process.env.JWT_SECRET, {
      expiresIn: "2h",
    });

    user.token = token;
    user.password = undefined;

    const options = {
      expires: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000),
      httpOnly: true,
      sameSite: "lax",
    };

    return res.cookie("token", token, options).status(200).json({
      success: true,
      message: "Login successful.",
      token,
      user,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Login failure. Please try again.",
      error: error.message,
    });
  }
};

exports.changePassword = async (req, res) => {
  try {
    const userId = req.user?.id;
    const { oldPassword, newPassword, confirmNewPassword } = req.body;

    if (!userId || !oldPassword || !newPassword || !confirmNewPassword) {
      return res.status(400).json({
        success: false,
        message: "userId, oldPassword, newPassword and confirmNewPassword are required.",
      });
    }

    if (newPassword !== confirmNewPassword) {
      return res.status(400).json({
        success: false,
        message: "New password and confirm password do not match.",
      });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: "User not found.",
      });
    }

    const isMatch = await bcrypt.compare(oldPassword, user.password);
    if (!isMatch) {
      return res.status(401).json({
        success: false,
        message: "Old password is incorrect.",
      });
    }

    const hashedPassword = await bcrypt.hash(newPassword, 10);
    user.password = hashedPassword;
    await user.save();

    return res.status(200).json({
      success: true,
      message: "Password changed successfully.",
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Failed to change password.",
      error: error.message,
    });
  }
};

```

## backend\controllers\Category.js

```javascript
const mongoose = require("mongoose");

const Category = require("../models/Category");
const Course = require("../models/Course");

exports.createCategory = async (req, res) => {
  try {
    const { name, description } = req.body;

    if (!name || !description) {
      return res.status(400).json({
        success: false,
        message: "name and description are required.",
      });
    }

    const existing = await Category.findOne({ name });
    if (existing) {
      return res.status(409).json({
        success: false,
        message: "Category with this name already exists.",
      });
    }

    const categoryDetails = await Category.create({
      name,
      description,
    });

    return res.status(201).json({
      success: true,
      message: "Category created successfully.",
      category: categoryDetails,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Failed to create category.",
      error: err.message,
    });
  }
};

exports.showAllCategories = async (req, res) => {
  try {
    const allCategories = await Category.find(
      {},
      { name: true, description: true },
    );

    return res.status(200).json({
      success: true,
      message: "Returned all categories successfully.",
      categories: allCategories,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Failed to fetch categories.",
      error: err.message,
    });
  }
};

// categoryPageDetails
exports.categoryPageDetails = async (req, res) => {
  try {
    const { categoryId } = req.body;

    if (!categoryId || !mongoose.Types.ObjectId.isValid(categoryId)) {
      return res.status(400).json({
        success: false,
        message: "Valid categoryId is required.",
      });
    }

    const selectedCategory = await Category.findById(categoryId)
      .populate("course")
      .exec();

    if (!selectedCategory) {
      return res.status(404).json({
        success: false,
        message: "Category not found.",
      });
    }

    const differentCategories = await Category.find({
      _id: { $ne: new mongoose.Types.ObjectId(categoryId) },
    })
      .populate("course")
      .exec();

    const topSellingCourses = await Course.find({})
      .sort({ totalStudentsEnrolled: -1 })
      .limit(5)
      .exec();

    return res.status(200).json({
      success: true,
      selectedCategory,
      differentCategories,
      topSellingCourses,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Failed to fetch category page details.",
      error: err.message,
    });
  }
};

```

## backend\controllers\Course.js

```javascript
const Course = require("../models/Course");
const Category = require("../models/Category");
const User = require("../models/User");
const { uploadImageToCloudinary } = require("../utils/imageUploader");

exports.createCourse = async (req, res) => {
  try {
    //fetch data
    const {
      courseName,
      courseDescription,
      whatYouWillLearn,
      price,
      tag,
      categoryId,
    } = req.body;

    //get thumbnail
    const thumbnail = req.files && req.files.thumbnailImage;
    const category = categoryId || tag;

    //validation
    if (
      !courseName ||
      !courseDescription ||
      !whatYouWillLearn ||
      !price ||
      !category ||
      !thumbnail
    ) {
      return res.status(400).json({
        success: false,
        message: "All fields are required for course creation.",
      });
    }

    //check instructor
    const userId = req.user && req.user.id;
    if (!userId) {
      return res.status(401).json({
        success: false,
        message: "Unauthorized.",
      });
    }

    const instructorDetails = await User.findById(userId);

    if (!instructorDetails) {
      return res.status(404).json({
        success: false,
        message: "instructor details not found.",
      });
    }

    //check given category is valid or not
    const categoryDetails = await Category.findById(category);
    if (!categoryDetails) {
      return res.status(404).json({
        success: false,
        message: "Category not found.",
      });
    }

    //upload to cloudinary
    const thumbnailImage = await uploadImageToCloudinary(
      thumbnail,
      process.env.FOLDER_NAME,
    );

    //create entry for newCourse
    const newCourse = await Course.create({
      courseName,
      courseDescription,
      instructor: instructorDetails._id,
      whatYouWillLearn,
      price,
      category: categoryDetails._id,
      thumbnail: thumbnailImage.secure_url,
    });

    //add new course to user schema of instructor
    await User.findByIdAndUpdate(
      instructorDetails._id,
      {
        $push: {
          courses: newCourse._id,
        },
      },
      {
        new: true,
      },
    );
    //update category schema
    await Category.findByIdAndUpdate(
      categoryDetails._id,
      {
        $push: {
          courses: newCourse._id,
        },
      },
      { new: true },
    );


    //return response
    return res.status(201).json({
      success: true,
      message: "Course Created Successfully",
      data: newCourse,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Failed to create course. " + err.message,
    });
  }
};



exports.getAllCourses = async (req, res) => {
  try {
    const allCourses = await Course.find(
      {},
      {
        courseName: true,
        courseDescription: true,
        price: true,
        instructor: true,
        thumbnail: true,
        ratingAndReviews: true,
        studentsEnrolled: true,
      },
    )
      .populate("instructor")
      .exec();

    return res.status(200).json({
      success: true,
      message: "Fetched all courses data.",
      allCourses,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Cannot fetch course data.",
      error: err.message,
    });
  }
};

// alias for older name
exports.showAllCourses = exports.getAllCourses;


exports.getCourseDetails = async (req, res) => {
  try {
    const { course_id, courseId } = req.body;
    const id = courseId || course_id;

    if (!id) {
      return res.status(400).json({
        success: false,
        message: "courseId is required.",
      });
    }

    const courseDetails = await Course.findById(id)
      .populate({
        path: "instructor",
        populate: { path: "additionalDetails" },
      })
      .populate("category")
      .populate("ratingAndReviews")
      .populate({
        path: "courseContent",
        populate: { path: "subSection" },
      })
      .exec();

    if (!courseDetails) {
      return res.status(404).json({
        success: false,
        message: "Could not find the course with the given courseId.",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Course details fetched successfully.",
      data: courseDetails,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Cannot fetch course details.",
      error: err.message,
    });
  }
};

exports.getFullCourseDetails = async (req, res) => {
  try {
    const { courseId, course_id } = req.body;
    const id = courseId || course_id;

    if (!id) {
      return res.status(400).json({
        success: false,
        message: "courseId is required.",
      });
    }

    const courseDetails = await Course.findById(id)
      .populate({ path: "instructor", populate: { path: "additionalDetails" } })
      .populate("category")
      .populate({ path: "courseContent", populate: { path: "subSection" } })
      .populate({ path: "ratingAndReviews", populate: { path: "user", select: "firstName lastName" } })
      .exec();

    if (!courseDetails) {
      return res.status(404).json({
        success: false,
        message: "Course not found.",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Full course details fetched successfully.",
      data: courseDetails,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Cannot fetch full course details.",
      error: err.message,
    });
  }
};

exports.editCourse = async (req, res) => {
  try {
    const instructorId = req.user?.id;
    const { courseId, course_id, courseName, courseDescription, whatYouWillLearn, price, categoryId, status, instructions, tag } = req.body;
    const id = courseId || course_id;

    if (!id) {
      return res.status(400).json({
        success: false,
        message: "courseId is required.",
      });
    }

    const course = await Course.findById(id);
    if (!course) {
      return res.status(404).json({
        success: false,
        message: "Course not found.",
      });
    }

    if (String(course.instructor) !== String(instructorId)) {
      return res.status(403).json({
        success: false,
        message: "You are not allowed to edit this course.",
      });
    }

    const updatePayload = {};
    if (courseName) updatePayload.courseName = courseName;
    if (courseDescription) updatePayload.courseDescription = courseDescription;
    if (whatYouWillLearn) updatePayload.whatYouWillLearn = whatYouWillLearn;
    if (typeof price !== "undefined") updatePayload.price = price;
    if (categoryId) updatePayload.category = categoryId;
    if (status) updatePayload.status = status;
    if (instructions) updatePayload.instructions = instructions;
    if (tag) updatePayload.tag = tag;

    const updatedCourse = await Course.findByIdAndUpdate(id, updatePayload, { new: true, runValidators: true });

    return res.status(200).json({
      success: true,
      message: "Course updated successfully.",
      data: updatedCourse,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Failed to edit course.",
      error: err.message,
    });
  }
};

exports.getInstructorCourses = async (req, res) => {
  try {
    const instructorId = req.user?.id;

    if (!instructorId) {
      return res.status(401).json({
        success: false,
        message: "Unauthorized access.",
      });
    }

    const instructorCourses = await Course.find({ instructor: instructorId })
      .populate("category")
      .populate("courseContent")
      .exec();

    return res.status(200).json({
      success: true,
      message: "Instructor courses fetched successfully.",
      courses: instructorCourses,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Failed to fetch instructor courses.",
      error: err.message,
    });
  }
};

exports.deleteCourse = async (req, res) => {
  try {
    const instructorId = req.user?.id;
    const { courseId, course_id } = req.body;
    const id = courseId || course_id;

    if (!id) {
      return res.status(400).json({
        success: false,
        message: "courseId is required.",
      });
    }

    const course = await Course.findById(id);
    if (!course) {
      return res.status(404).json({
        success: false,
        message: "Course not found.",
      });
    }

    if (String(course.instructor) !== String(instructorId)) {
      return res.status(403).json({
        success: false,
        message: "You are not allowed to delete this course.",
      });
    }

    await Course.findByIdAndDelete(id);

    return res.status(200).json({
      success: true,
      message: "Course deleted successfully.",
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Failed to delete course.",
      error: err.message,
    });
  }
};
```

## backend\controllers\courseProgress.js

```javascript
const CourseProgress = require("../models/CourseProgress");
const Course = require("../models/Course");
const User = require("../models/User");

exports.updateCourseProgress = async (req, res) => {
  try {
    const userId = req.user?.id;
    const { courseId, subSectionId } = req.body;

    if (!userId || !courseId || !subSectionId) {
      return res.status(400).json({
        success: false,
        message: "userId, courseId and subSectionId are required.",
      });
    }

    const course = await Course.findById(courseId);
    if (!course) {
      return res.status(404).json({
        success: false,
        message: "Course not found.",
      });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: "User not found.",
      });
    }

    let progress = await CourseProgress.findOne({ courseId });
    if (!progress) {
      progress = await CourseProgress.create({ courseId, completedVideos: [subSectionId] });
      user.courseProgess = user.courseProgess || [];
      if (!user.courseProgess.includes(progress._id)) {
        user.courseProgess.push(progress._id);
        await user.save();
      }
    } else {
      if (!progress.completedVideos.includes(subSectionId)) {
        progress.completedVideos.push(subSectionId);
        await progress.save();
      }
    }

    return res.status(200).json({
      success: true,
      message: "Course progress updated successfully.",
      progress,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Failed to update course progress.",
      error: err.message,
    });
  }
};


```

## backend\controllers\Payments.js

```javascript
const mongoose = require("mongoose");
const crypto = require("crypto");

const { instance } = require("../config/razorpay");
const Course = require("../models/Course");
const User = require("../models/User");
const mailSender = require("../utils/mailSender");
const { courseEnrollmentEmail } = require("../mail/templates/courseEnrollmentEmail");

// capture payment and initiate order (supports card/UPI via Razorpay)
exports.capturePayment = async (req, res) => {
  try {
    const { courseId } = req.body;
    const userId = req.user && req.user.id;

    if (!courseId || !userId) {
      return res.status(400).json({
        success: false,
        message: "courseId and authenticated user are required.",
      });
    }

    const course = await Course.findById(courseId);
    if (!course) {
      return res.status(404).json({
        success: false,
        message: "Course not found.",
      });
    }

    const uid = new mongoose.Types.ObjectId(userId);
    if (course.studentsEnrolled.includes(uid)) {
      return res.status(409).json({
        success: false,
        message: "Student already enrolled in this course.",
      });
    }

    const amount = course.price;
    const currency = "INR";

    const options = {
      amount: amount * 100,
      currency,
      receipt: `${Date.now()}`,
      notes: {
        courseId,
        userId,
      },
    };

    const paymentResponse = await instance.orders.create(options);

    return res.status(200).json({
      success: true,
      message: "Payment order created successfully.",
      courseName: course.courseName,
      courseDescription: course.courseDescription,
      thumbnail: course.thumbnail,
      orderId: paymentResponse.id,
      currency: paymentResponse.currency,
      amount: paymentResponse.amount,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Failed to capture payment.",
      error: error.message,
    });
  }
};

// webhook handler to verify Razorpay signature and enroll student
exports.verifySignature = async (req, res) => {
  try {
    const webhookSecret = process.env.RAZORPAY_WEBHOOK_SECRET;

    const signature = req.headers["x-razorpay-signature"];
    const shasum = crypto.createHmac("sha256", webhookSecret);
    shasum.update(JSON.stringify(req.body));
    const digest = shasum.digest("hex");

    if (digest !== signature) {
      return res.status(400).json({
        success: false,
        message: "Invalid payment signature.",
      });
    }

    const { courseId, userId } = req.body.payload.payment.entity.notes;

    const enrolledCourse = await Course.findByIdAndUpdate(
      courseId,
      { $addToSet: { studentsEnrolled: userId } },
      { new: true },
    );

    const enrolledStudent = await User.findByIdAndUpdate(
      userId,
      { $addToSet: { courses: courseId } },
      { new: true },
    );

    if (!enrolledCourse || !enrolledStudent) {
      return res.status(400).json({
        success: false,
        message: "Failed to enroll student after payment.",
      });
    }

    const body = courseEnrollmentEmail(
      enrolledStudent.firstName,
      enrolledCourse.courseName,
    );
    await mailSender(enrolledStudent.email, "Course Enrollment Confirmed", body);

    return res.status(200).json({
      success: true,
      message: "Payment verified and course enrollment completed.",
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Error while verifying payment signature.",
      error: error.message,
    });
  }
};

// alias used by existing routes
exports.verifyPayment = exports.verifySignature;

// optional explicit success email handler (idempotent)
exports.sendPaymentSuccessEmail = async (req, res) => {
  try {
    const { email, courseName } = req.body;

    if (!email || !courseName) {
      return res.status(400).json({
        success: false,
        message: "email and courseName are required.",
      });
    }

    await mailSender(
      email,
      "Payment successful",
      `You have successfully enrolled in ${courseName}.`,
    );

    return res.status(200).json({
      success: true,
      message: "Payment success email sent.",
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Failed to send payment success email.",
      error: error.message,
    });
  }
};


```

## backend\controllers\Profile.js

```javascript
const Profile = require('../models/Profile')
const User = require('../models/User')
const Course = require('../models/Course')

exports.updateProfile = async (req, res) => {
  try {
    const { dateOfBirth = "", about = "", contactNumber, gender } = req.body;
    const id = req.user && req.user.id;

    if (!gender || !contactNumber || !id) {
      return res.status(400).json({
        success: false,
        message: "gender, contactNumber and user id are required for updating profile.",
      });
    }

    const userDetails = await User.findById(id);
    if (!userDetails) {
      return res.status(404).json({
        success: false,
        message: "User not found.",
      });
    }

    const profileId = userDetails.additionalDetails;
    const profileDetails = await Profile.findById(profileId);
    if (!profileDetails) {
      return res.status(404).json({
        success: false,
        message: "Profile not found.",
      });
    }

    profileDetails.dateOfBirth = dateOfBirth;
    profileDetails.about = about;
    profileDetails.gender = gender;
    profileDetails.contactNumber = contactNumber;
    await profileDetails.save();

    return res.status(200).json({
      success: true,
      message: "Profile updated successfully.",
      profile: profileDetails,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Failed to update profile.",
      error: error.message,
    });
  }
};


exports.deleteAccount=async (req,res)=>{
    try{

        //get id
        const id=req.user.id;
        //validate
        const userDetails=await User.findById(id);
        if(!userDetails){
            return res.status(404).json({
            success:false,
            message:'User not found for deletion.'
        })
        }
        //delete profile
        await Profile.findByIdAndDelete({_id:userDetails.additionalDetails})

        //enrolled account se bhi delete hojaye
        //how can we schdeule this operation

        //delete user

        await User.findByIdAndDelete({_id:id})

        
        //return res
        return res.status(200).json({
            success:true,
            message:"Account deleted successfully."
        })

    }catch(error){
        return res.status(500).json({
            success:false,
            message:error.message
        })
    }
}

exports.getAllUserDetails = async (req, res) => {
  try {
    const id = req.user && req.user.id;
    if (!id) {
      return res.status(401).json({
        success: false,
        message: "Unauthorized access.",
      });
    }

    const userDetails = await User.findById(id)
      .populate("additionalDetails")
      .exec();

    if (!userDetails) {
      return res.status(404).json({
        success: false,
        message: "User not found.",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Account fetched successfully.",
      user: userDetails,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Failed to fetch user details.",
      error: error.message,
    });
  }
};

const { uploadImageToCloudinary } = require("../utils/imageUploader");

exports.updateDisplayPicture = async (req, res) => {
  try {
    const userId = req.user?.id;
    const file = req.files?.displayPicture;

    if (!userId || !file) {
      return res.status(400).json({
        success: false,
        message: "user id and displayPicture file are required.",
      });
    }

    const uploadResult = await uploadImageToCloudinary(file, process.env.FOLDER_NAME);

    const updatedUser = await User.findByIdAndUpdate(
      userId,
      { image: uploadResult.secure_url },
      { new: true }
    );
    

    if (!updatedUser) {
      return res.status(404).json({
        success: false,
        message: "User not found.",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Display picture updated successfully.",
      imageUrl: updatedUser.image,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Failed to update display picture.",
      error: err.message,
    });
  }
};

exports.getEnrolledCourses = async (req, res) => {
  try {
    const userId = req.user?.id;
    if (!userId) {
      return res.status(401).json({
        success: false,
        message: "Unauthorized access.",
      });
    }

    const user = await User.findById(userId).populate({
      path: "courses",
      populate: [
        { path: "instructor", select: "firstName lastName" },
        { path: "category" },
      ],
    });

    if (!user) {
      return res.status(404).json({
        success: false,
        message: "User not found.",
      });
    }

    return res.status(200).json({
      success: true,
      message: "Enrolled courses fetched successfully.",
      courses: user.courses,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Failed to fetch enrolled courses.",
      error: err.message,
    });
  }
};

exports.instructorDashboard = async (req, res) => {
  try {
    const instructorId = req.user?.id;
    if (!instructorId) {
      return res.status(401).json({
        success: false,
        message: "Unauthorized access.",
      });
    }

    const instructorCourses = await Course.find({ instructor: instructorId })
      .populate("studentsEnrolled")
      .exec();

    const totalCourses = instructorCourses.length;
    const totalStudents = instructorCourses.reduce(
      (sum, course) => sum + (course.studentsEnrolled?.length || 0),
      0,
    );

    return res.status(200).json({
      success: true,
      message: "Instructor dashboard data fetched successfully.",
      data: {
        totalCourses,
        totalStudents,
        courses: instructorCourses,
      },
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Failed to fetch instructor dashboard.",
      error: err.message,
    });
  }
};
```

## backend\controllers\RatingAndReview.js

```javascript
const RatingAndReview = require("../models/RatingAndReview");
const Course = require("../models/Course");

//createRating
exports.createRating = async (req, res) => {
  try {
    //getData
    const userId = req.user.id;
    //fetch
    const { rating, review, courseId } = req.body;
    //check if user exists in enrolled
    const courseDetails = await Course.findOne({
      _id: courseId,
      studentEnrolled: userId,
    });
    if (!courseDetails) {
      return res.status(500).json({
        success: false,
        message: "Student not enrolled.",
      });
    }
    //check if user already revewid
    const alreadyReviewed = await RatingAndReview.findOne({
      user: userId,
      course: courseId,
    });
    if (!courseDetails) {
      return res.status(500).json({
        success: false,
        message: "Already Reviewed.",
      });
    }
    //create rating
    const ratingReview = await RatingAndReview.create({
      rating,
      review,
      course: courseId,
      user: userId,
    });
    //update Course model
    await Course.findByIdAndUpdate(
      courseId,
      {
        $push: {
          ratingAndReviews: ratingReview._id,
        },
      },
      {
        new: true,
      },
    );
    //return res
    return res.status(200).json({
      success: true,
      message: "Created review successfully.",
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: err.message,
    });
  }
};

//getAvg
exports.getAverageRating = async (req, res) => {
  try {
    const { courseId } = req.body;

    if (!mongoose.Types.ObjectId.isValid(courseId)) {
      return res.status(400).json({
        success: false,
        message: "Invalid Course ID",
      });
    }

    const result = await RatingAndReview.aggregate([
      {
        $match: {
          course: new mongoose.Types.ObjectId(courseId),
        },
      },
      {
        $group: {
          _id: null,
          avgRating: { $avg: "$rating" },
        },
      },
    ]);

    const averageRating = result[0]?.avgRating
      ? Number(result[0].avgRating.toFixed(1))
      : 0;

    return res.status(200).json({
      success: true,
      averageRating,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Failed to fetch average rating",
      error: error.message,
    });
  }
};

//getAll
exports.getAllRating = async (req, res) => {
  try {
    const allReviews = await RatingAndReview.find({})
      .sort({ rating: "desc" })
      .populate({ path: "user", select: "firstName lastName email image" })
      .populate({path: "course",select:"courseName"}).exec()

    return res.status(200).json({
        success:true,
        message:"All reviews fetched successfully",
        allReviews
    })
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Failed to fetch all rating",
      error: error.message,
    });
  }
};

```

## backend\controllers\ResetPassword.js

```javascript
const crypto = require("crypto");
const bcrypt = require("bcrypt");

const User = require("../models/User");
const mailSender = require("../utils/mailSender");
const { resetPasswordEmail } = require("../mail/templates/resetPasswordEmail");

//resetPasswordToken
exports.resetPasswordToken = async (req, res) => {
  try {
    //get email from req body
    const email = req.body.email;

    //check user for this email , email validation
    const user = await User.findOne({ email: email });
    if (!user) {
      return res.status(400).json({
        success: false,
        message: "User doesnt exist.",
      });
    }
    //generate token
    const token = crypto.randomUUID();

    //update user by adding token and expiration time
    await User.findOneAndUpdate(
      { email },
      {
        token: token,
        resetPasswordExpires: Date.now() + 5 * 60 * 1000,
      },
      { new: true },
    );

    //create url
    const url = `${process.env.FRONTEND_URL || "http://localhost:3000"}/update-password/${token}`;
    const body = resetPasswordEmail(url);
    //send mail containing the url
    await mailSender(email, "Password Reset Link", body);
    // return response

    return res.status(200).json({
      success: true,
      message: "Email send successfully, Please check email and changePassword",
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Something went wrong while reseting password.",
      error: err.message,
    });
  }
};

//resetPassword

exports.resetPassword = async (req, res) => {
  try {
    //data fetch
    const { password, confirmPassword, token } = req.body;
    if (!password || !confirmPassword || !token) {
      return res.status(400).json({
        success: false,
        message: "password, confirmPassword and token are required.",
      });
    }
    //validate
    if (password !== confirmPassword) {
      return res.status(400).json({
        success: false,
        message: "Password not matching.",
      });
    }
    //User entry update passwrord update using token
    //if no entry invalid token
    const userDetails = await User.findOne({ token: token });
    if (!userDetails) {
      return res.status(400).json({
        success: false,
        message: "Token invalid.",
      });
    }
    
    //if token time expired already
    if (userDetails.resetPasswordExpires < Date.now()) {
      return res.status(400).json({
        success: false,
        message: "Token expired while resetting.",
      });
    }

    //password hashedPassword
    const hashedPassword = await bcrypt.hash(password, 10);

    //update password
    await User.findOneAndUpdate({
      token: token,
    }, {
      password: hashedPassword,
      token: undefined,
      resetPasswordExpires: undefined,
    }, { new: true });
    //response
    return res.status(200).json({
      success: true,
      message: "Resetting password successfully.",
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Something went wrong while reseting password.",
      error: error.message,
    });
  }
};

```

## backend\controllers\Section.js

```javascript
const Section = require("../models/Section");
const Course = require("../models/Course");

exports.createSection = async (req, res) => {
  try {
    const { sectionName, courseId } = req.body;

    if (!sectionName || !courseId) {
      return res.status(400).json({
        success: false,
        message: "sectionName and courseId are required",
      });
    }

    const newSection = await Section.create({ sectionName });

    const updatedCourse = await Course.findByIdAndUpdate(
      courseId,
      {
        $push: { courseContent: newSection._id },
      },
      { new: true }
    ).populate({
      path: "courseContent",
      populate: { path: "subSection" },
    });

    return res.status(201).json({
      success: true,
      message: "Section created successfully",
      updatedCourse,
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Section creation failed",
      error: error.message,
    });
  }
};


exports.updateSection = async (req, res) => {
  try {
    const { sectionName, sectionId } = req.body;

    if (!sectionName || !sectionId) {
      return res.status(400).json({
        success: false,
        message: "sectionName and sectionId are required",
      });
    }

    const updatedSectionDetails = await Section.findByIdAndUpdate(
      sectionId,
      { sectionName },
      { new: true, runValidators: true }
    );

    return res.status(200).json({
      success: true,
      message: "Section updated successfully",
      updatedSectionDetails,
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Section update failed",
      error: error.message,
    });
  }
};


exports.deleteSection = async (req, res) => {
  try {
    const { sectionId, courseId } = req.body;

    if (!sectionId || !courseId) {
      return res.status(400).json({
        success: false,
        message: "sectionId and courseId are required",
      });
    }

    // remove section reference from course
    await Course.findByIdAndUpdate(courseId, {
      $pull: { courseContent: sectionId },
    });

    // delete section after removing reference from course
    await Section.findByIdAndDelete(sectionId);
    return res.status(200).json({
      success: true,
      message: "Section deleted successfully",
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Section deletion failed",
      error: error.message,
    });
  }
};

```

## backend\controllers\SubSection.js

```javascript
const SubSection = require("../models/SubSection");
const Section = require("../models/Section");
const { uploadImageToCloudinary } = require("../utils/imageUploader");

exports.createSubSection = async (req, res) => {
  try {
    // fetch data
    const { sectionId, title, timeDuration, description } = req.body;

    // fetch video
    const video = req.files?.videoFile;

    // validation
    if (!sectionId || !title || !timeDuration || !description || !video) {
      return res.status(400).json({
        success: false,
        message: "All fields are required for SubSection",
      });
    }

    // upload video to cloudinary
    const uploadDetails = await uploadImageToCloudinary(
      video,
      process.env.FOLDER_NAME
    );

    // create subsection
    const subSectionDetails = await SubSection.create({
      title,
      timeDuration,
      description,
      videoUrl: uploadDetails.secure_url,
    });

    // update section with subsection id
    const updatedSection = await Section.findByIdAndUpdate(
      sectionId,
      {
        $push: {
          subSection: subSectionDetails._id,
        },
      },
      { new: true }
    ).populate("subSection");

    // return response
    return res.status(201).json({
      success: true,
      message: "SubSection created successfully",
      updatedSection,
    });

  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "SubSection creation failed",
      error: err.message,
    });
  }
};

exports.updateSubSection = async (req, res) => {
  try {
    const { subSectionId, title, timeDuration, description, additionalUrl } = req.body;
    const video = req.files?.videoFile;

    if (!subSectionId) {
      return res.status(400).json({
        success: false,
        message: "subSectionId is required.",
      });
    }

    const updatePayload = {};
    if (title) updatePayload.title = title;
    if (timeDuration) updatePayload.timeDuration = timeDuration;
    if (description) updatePayload.description = description;
    if (additionalUrl) updatePayload.additionalUrl = additionalUrl;

    if (video) {
      const uploadDetails = await uploadImageToCloudinary(video, process.env.FOLDER_NAME);
      updatePayload.videoUrl = uploadDetails.secure_url;
    }

    const updatedSubSection = await SubSection.findByIdAndUpdate(subSectionId, updatePayload, {
      new: true,
      runValidators: true,
    });

    if (!updatedSubSection) {
      return res.status(404).json({
        success: false,
        message: "SubSection not found.",
      });
    }

    return res.status(200).json({
      success: true,
      message: "SubSection updated successfully.",
      updatedSubSection,
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "Failed to update SubSection.",
      error: err.message,
    });
  }
};



exports.deleteSubSection = async (req, res) => {
  try {
    const { subSectionId, sectionId } = req.body;

    if (!subSectionId || !sectionId) {
      return res.status(400).json({
        success: false,
        message: "subSectionId and sectionId are required",
      });
    }

    // remove subsection reference from section
    await Section.findByIdAndUpdate(
      sectionId,
      {
        $pull: { subSection: subSectionId },
      }
    );

    // delete subsection
    await SubSection.findByIdAndDelete(subSectionId);

    return res.status(200).json({
      success: true,
      message: "SubSection deleted successfully",
    });

  } catch (err) {
    return res.status(500).json({
      success: false,
      message: "SubSection deletion failed",
      error: err.message,
    });
  }
};

// NOTE: duplicate deleteSubSection removed

```

## backend\file.json

```json
{
  "info": {
    "name": "StudyNotion API",
    "description": "Backend routes for StudyNotion manual testing. Base URL: http://localhost:4000",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. AUTH & RESET PASSWORD",
      "item": [
        {
          "name": "1.1 Login",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"email\": \"user@example.com\",\n  \"password\": \"Password123\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/auth/login", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "auth", "login"] }
          }
        },
        {
          "name": "1.2 Signup",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"firstName\": \"Aryan\",\n  \"lastName\": \"Akhare\",\n  \"email\": \"user@example.com\",\n  \"password\": \"Password123\",\n  \"confirmPassword\": \"Password123\",\n  \"accountType\": \"Student\",\n  \"contactNumber\": \"9999999999\",\n  \"otp\": \"123456\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/auth/signup", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "auth", "signup"] }
          }
        },
        {
          "name": "1.3 Send OTP",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"email\": \"user@example.com\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/auth/sendotp", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "auth", "sendotp"] }
          }
        },
        {
          "name": "1.4 Change Password",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"oldPassword\": \"OldPass123\",\n  \"newPassword\": \"NewPass123\",\n  \"confirmNewPassword\": \"NewPass123\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/auth/changepassword", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "auth", "changepassword"] }
          }
        },
        {
          "name": "1.5 Create Reset Password Token",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"email\": \"user@example.com\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/auth/reset-password-token", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "auth", "reset-password-token"] }
          }
        },
        {
          "name": "1.6 Reset Password",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"password\": \"NewPass123\",\n  \"confirmPassword\": \"NewPass123\",\n  \"token\": \"<RESET_TOKEN_FROM_EMAIL>\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/auth/reset-password", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "auth", "reset-password"] }
          }
        }
      ]
    },
    {
      "name": "2. PROFILE ROUTES",
      "item": [
        {
          "name": "2.1 Delete Account",
          "request": {
            "method": "DELETE",
            "header": [
              { "key": "Cookie", "value": "token=<JWT_HERE>" }
            ],
            "url": { "raw": "http://localhost:4000/api/v1/profile/deleteProfile", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "profile", "deleteProfile"] }
          }
        },
        {
          "name": "2.2 Update Profile",
          "request": {
            "method": "PUT",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"dateOfBirth\": \"2000-01-01\",\n  \"about\": \"About me\",\n  \"contactNumber\": \"9999999999\",\n  \"gender\": \"Male\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/profile/updateProfile", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "profile", "updateProfile"] }
          }
        },
        {
          "name": "2.3 Get User Details",
          "request": {
            "method": "GET",
            "header": [
              { "key": "Cookie", "value": "token=<JWT_HERE>" }
            ],
            "url": { "raw": "http://localhost:4000/api/v1/profile/getUserDetails", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "profile", "getUserDetails"] }
          }
        },
        {
          "name": "2.4 Get Enrolled Courses",
          "request": {
            "method": "GET",
            "header": [
              { "key": "Cookie", "value": "token=<JWT_HERE>" }
            ],
            "url": { "raw": "http://localhost:4000/api/v1/profile/getEnrolledCourses", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "profile", "getEnrolledCourses"] }
          }
        },
        {
          "name": "2.5 Update Display Picture",
          "request": {
            "method": "PUT",
            "header": [
              { "key": "Cookie", "value": "token=<JWT_HERE>" }
            ],
            "body": {
              "mode": "formdata",
              "formdata": [
                { "key": "displayPicture", "type": "file", "src": "" }
              ]
            },
            "url": { "raw": "http://localhost:4000/api/v1/profile/updateDisplayPicture", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "profile", "updateDisplayPicture"] }
          }
        },
        {
          "name": "2.6 Instructor Dashboard",
          "request": {
            "method": "GET",
            "header": [
              { "key": "Cookie", "value": "token=<INSTRUCTOR_JWT_HERE>" }
            ],
            "url": { "raw": "http://localhost:4000/api/v1/profile/instructorDashboard", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "profile", "instructorDashboard"] }
          }
        }
      ]
    },
    {
      "name": "3. COURSE & CATEGORY ROUTES",
      "item": [
        {
          "name": "3.1 Create Course",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Cookie", "value": "token=<INSTRUCTOR_JWT_HERE>" }
            ],
            "body": {
              "mode": "formdata",
              "formdata": [
                { "key": "courseName", "value": "My Course", "type": "text" },
                { "key": "courseDescription", "value": "Nice course", "type": "text" },
                { "key": "whatYouWillLearn", "value": "Skills list", "type": "text" },
                { "key": "price", "value": "999", "type": "text" },
                { "key": "categoryId", "value": "<CATEGORY_ID>", "type": "text" },
                { "key": "thumbnailImage", "type": "file", "src": "" }
              ]
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/createCourse", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "createCourse"] }
          }
        },
        {
          "name": "3.2 Add Section",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<INSTRUCTOR_JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"sectionName\": \"Introduction\",\n  \"courseId\": \"<COURSE_ID>\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/addSection", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "addSection"] }
          }
        },
        {
          "name": "3.3 Update Section",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<INSTRUCTOR_JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"sectionName\": \"Updated name\",\n  \"sectionId\": \"<SECTION_ID>\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/updateSection", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "updateSection"] }
          }
        },
        {
          "name": "3.4 Delete Section",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<INSTRUCTOR_JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"sectionId\": \"<SECTION_ID>\",\n  \"courseId\": \"<COURSE_ID>\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/deleteSection", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "deleteSection"] }
          }
        },
        {
          "name": "3.5 Add SubSection",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Cookie", "value": "token=<INSTRUCTOR_JWT_HERE>" }
            ],
            "body": {
              "mode": "formdata",
              "formdata": [
                { "key": "sectionId", "value": "<SECTION_ID>", "type": "text" },
                { "key": "title", "value": "Lecture 1", "type": "text" },
                { "key": "timeDuration", "value": "10:00", "type": "text" },
                { "key": "description", "value": "Intro video", "type": "text" },
                { "key": "videoFile", "type": "file", "src": "" }
              ]
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/addSubSection", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "addSubSection"] }
          }
        },
        {
          "name": "3.6 Update SubSection",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<INSTRUCTOR_JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"subSectionId\": \"<SUBSECTION_ID>\",\n  \"sectionId\": \"<SECTION_ID>\",\n  \"title\": \"Updated title\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/updateSubSection", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "updateSubSection"] }
          }
        },
        {
          "name": "3.7 Delete SubSection",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<INSTRUCTOR_JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"subSectionId\": \"<SUBSECTION_ID>\",\n  \"sectionId\": \"<SECTION_ID>\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/deleteSubSection", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "deleteSubSection"] }
          }
        },
        {
          "name": "3.8 Get All Courses",
          "request": {
            "method": "GET",
            "url": { "raw": "http://localhost:4000/api/v1/course/getAllCourses", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "getAllCourses"] }
          }
        },
        {
          "name": "3.9 Get Course Details",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"courseId\": \"<COURSE_ID>\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/getCourseDetails", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "getCourseDetails"] }
          }
        },
        {
          "name": "3.10 Get Full Course Details",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"courseId\": \"<COURSE_ID>\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/getFullCourseDetails", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "getFullCourseDetails"] }
          }
        },
        {
          "name": "3.11 Edit Course",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<INSTRUCTOR_JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"courseId\": \"<COURSE_ID>\",\n  \"updates\": {}\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/editCourse", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "editCourse"] }
          }
        },
        {
          "name": "3.12 Get Instructor Courses",
          "request": {
            "method": "GET",
            "header": [
              { "key": "Cookie", "value": "token=<INSTRUCTOR_JWT_HERE>" }
            ],
            "url": { "raw": "http://localhost:4000/api/v1/course/getInstructorCourses", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "getInstructorCourses"] }
          }
        },
        {
          "name": "3.13 Delete Course",
          "request": {
            "method": "DELETE",
            "header": [
              { "key": "Content-Type", "value": "application/json" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"courseId\": \"<COURSE_ID>\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/deleteCourse", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "deleteCourse"] }
          }
        },
        {
          "name": "3.14 Update Course Progress",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<STUDENT_JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"courseId\": \"<COURSE_ID>\",\n  \"subSectionId\": \"<SUBSECTION_ID>\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/updateCourseProgress", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "updateCourseProgress"] }
          }
        },
        {
          "name": "3.15 Create Category",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<ADMIN_JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"name\": \"Dev\",\n  \"description\": \"Dev courses\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/createCategory", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "createCategory"] }
          }
        },
        {
          "name": "3.16 Show All Categories",
          "request": {
            "method": "GET",
            "url": { "raw": "http://localhost:4000/api/v1/course/showAllCategories", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "showAllCategories"] }
          }
        },
        {
          "name": "3.17 Get Category Page Details",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"categoryId\": \"<CATEGORY_ID>\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/getCategoryPageDetails", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "getCategoryPageDetails"] }
          }
        },
        {
          "name": "3.18 Create Rating",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<STUDENT_JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"courseId\": \"<COURSE_ID>\",\n  \"rating\": 5,\n  \"review\": \"Great course\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/createRating", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "createRating"] }
          }
        },
        {
          "name": "3.19 Get Average Rating",
          "request": {
            "method": "GET",
            "header": [
              { "key": "Content-Type", "value": "application/json" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"courseId\": \"<COURSE_ID>\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/course/getAverageRating", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "getAverageRating"] }
          }
        },
        {
          "name": "3.20 Get All Reviews",
          "request": {
            "method": "GET",
            "url": { "raw": "http://localhost:4000/api/v1/course/getReviews", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "course", "getReviews"] }
          }
        }
      ]
    },
    {
      "name": "4. PAYMENT / RAZORPAY ROUTES",
      "item": [
        {
          "name": "4.1 Capture Payment",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<STUDENT_JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"courseId\": \"<COURSE_ID>\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/payments/capturePayment", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "payments", "capturePayment"] }
          }
        },
        {
          "name": "4.2 Verify Payment",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "X-Razorpay-Signature", "value": "<SIGNATURE_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"payload\": {\n    \"payment\": {\n      \"entity\": {\n        \"notes\": {\n          \"courseId\": \"<COURSE_ID>\",\n          \"userId\": \"<USER_ID>\"\n        }\n      }\n    }\n  }\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/payments/verifyPayment", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "payments", "verifyPayment"] }
          }
        },
        {
          "name": "4.3 Send Payment Success Email",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Cookie", "value": "token=<STUDENT_JWT_HERE>" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"email\": \"user@example.com\",\n  \"courseName\": \"My Course\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": { "raw": "http://localhost:4000/api/v1/payments/sendPaymentSuccessEmail", "protocol": "http", "host": ["localhost"], "port": "4000", "path": ["api", "v1", "payments", "sendPaymentSuccessEmail"] }
          }
        }
      ]
    }
  ]
}
```

## backend\index.js

```javascript
const express = require("express");
const cookieParser = require("cookie-parser");
const cors = require("cors");
const fileUpload = require("express-fileupload");
const dotenv = require("dotenv");

const userRoutes = require("./routes/User");
const profileRoutes = require("./routes/Profile");
const paymentRoutes = require("./routes/Payments");
const courseRoutes = require("./routes/Course");

const database = require("./config/database");
const { cloudinaryConnect } = require("./config/cloudinary");

dotenv.config();

const app = express();
const PORT = process.env.PORT || 4000;

// db connect
database.connect();

// middlewares
app.use(express.json());
app.use(cookieParser());
app.use(
  cors({
    origin: process.env.FRONTEND_URL || "http://localhost:3000",
    credentials: true,
  }),
);

app.use(
  fileUpload({
    useTempFiles: true,
    tempFileDir: "/tmp",
  }),
);

// cloudinary connect
cloudinaryConnect();

// routes
app.use("/api/v1/auth", userRoutes);
app.use("/api/v1/profile", profileRoutes);
app.use("/api/v1/course", courseRoutes);
app.use("/api/v1/payments", paymentRoutes);

// default route
app.get("/", (req, res) => {
  return res.status(200).json({
    success: true,
    message: "Your server is up and running...",
  });
});

// fallback for unknown routes
app.use((req, res) => {
  return res.status(404).json({
    success: false,
    message: "Route not found",
  });
});

app.listen(PORT, () => {
  console.log(`App running successfully on port ${PORT}.`);
});
```

## backend\mail\templates\courseEnrollmentEmail.js

```javascript
exports.courseEnrollmentEmail = (studentName, courseName) => {
  return `
    <div style="font-family: sans-serif; line-height: 1.6;">
      <h2>Congratulations, ${studentName || "Learner"}!</h2>
      <p>You have successfully enrolled in <strong>${courseName}</strong> on StudyNotion.</p>
      <p>You can now start learning from your dashboard.</p>
      <p style="margin-top:16px;">Happy learning,<br/>StudyNotion Team</p>
    </div>
  `;
};


```

## backend\mail\templates\resetPasswordEmail.js

```javascript
exports.resetPasswordEmail = (resetUrl) => {
  return `
    <div style="font-family: sans-serif; line-height: 1.6;">
      <h2>Password Reset Request</h2>
      <p>We received a request to reset your StudyNotion password.</p>
      <p>Click the link below to set a new password (valid for 5 minutes):</p>
      <p><a href="${resetUrl}" target="_blank">${resetUrl}</a></p>
      <p>If you did not request this, you can safely ignore this email.</p>
      <p style="margin-top:16px;">Regards,<br/>StudyNotion Team</p>
    </div>
  `;
};


```

## backend\mail\templates\verificationEmail.js

```javascript
exports.verificationEmail = (otp) => {
  return `
    <div style="font-family: sans-serif; line-height: 1.6;">
      <h2>Verify your email</h2>
      <p>Your OTP for StudyNotion is:</p>
      <p style="font-size: 24px; font-weight: bold;">${otp}</p>
      <p>This code is valid for 5 minutes. Do not share it with anyone.</p>
      <p style="margin-top:16px;">Thanks,<br/>StudyNotion Team</p>
    </div>
  `;
};


```

## backend\middlewares\auth.js

```javascript
//Protected routes
const jwt = require("jsonwebtoken");
require("dotenv").config();
const user = require("../models/User");
//auth
exports.auth = async (req, res, next) => {
  try {
    // get token from cookie, body, or standard Authorization header
    const authHeader = req.header("Authorization") || req.header("authorization");
    let token = req.cookies?.token || req.body?.token || null;

    if (!token && authHeader) {
      token = authHeader.replace(/^Bearer\s+/i, "").trim();
    }

    if (!token) {
      return res.status(401).json({
        success: false,
        message: "Token is missing.",
      });
    }
    //verify token
    try {
      const decode = jwt.verify(token, process.env.JWT_SECRET);
      console.log(decode);
      req.user = decode;
    } catch (error) {
      return res.status(401).json({
        success: false,
        message: "Token is invalid.",
      });
      
    }
    next();
  } catch (error) {
    return res.status(401).json({
      success: false,
      message: "Something went wrong while validating token.",
    });
  }
};

//isStudent
exports.isStudent=async(req,res,next)=>{
    try{
        if(req.user.accountType!=="Student"){
            return res.status(401).json({
                success: false,
                message: "Protected route for Students only.",
            })
        }
    }
    catch(err){
        return res.status(401).json({
      success: false,
      message: "User role cannot be verified , please try again.",
    });
    }
}
//isAdmin
exports.isAdmin=async(req,res,next)=>{
    try{
        if(req.user.accountType!=="Admin"){
            return res.status(401).json({
                success: false,
                message: "Protected route for Admins only.",
            })
        }
    }
    catch(err){
        return res.status(401).json({
      success: false,
      message: "User role cannot be verified , please try again.",
    });
    }
}
//isInstructor
exports.isInstructor=async(req,res,next)=>{
    try{
        if(req.user.accountType!=="Instructor"){
            return res.status(401).json({
                success: false,
                message: "Protected route for Instructors only.",
            })
        }
    }
    catch(err){
        return res.status(401).json({
      success: false,
      message: "User role cannot be verified , please try again.",
    });
    }
}

```

## backend\models\Category.js

```javascript
const mongoose=require("mongoose");

const CategorySchema=new mongoose.Schema({
    name:{
        type:String,
        required:true
    },
    description:{
        type:String,
        required:true,
        trim:true
    },
    course:{
        type: mongoose.Schema.Types.ObjectId,
        ref:"Course"
    }
})
module.exports=mongoose.model("Category",CategorySchema)
```

## backend\models\Course.js

```javascript
const mongoose = require("mongoose");

const courseSchema = new mongoose.Schema({
  courseName: {
    type: String,
    required: true,
    trim: true,
  },
  courseDescription: {
    type: String,
    required: true,
    trim: true,
  },
  instructor: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "User",
    required: true,
  },
  whatYouWillLearn: {
    type: String,
    required: true,
  },
  courseContent: [
    {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Section",
    },
  ],
  ratingAndReviews: [
    {
      type: mongoose.Schema.Types.ObjectId,
      ref: "RatingAndReview",
    },
  ],
  price: {
    type: Number,
    required: true,
  },
  tag: {
    type: [String],
    ref: "Tag",
  },
  category:{
    type:mongoose.Schema.Types.ObjectId,
    ref:"Category"
  },
  instructions:{
    type:[String]
  },
  status:{
    type:String,
    enum:["Draft","Published"]
  },
  studentsEnrolled: [
    {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
  ],
});

module.exports = mongoose.model("Course", courseSchema);

```

## backend\models\CourseProgress.js

```javascript
const mongoose = require("mongoose");

const courseProgessSchema = new mongoose.Schema({
  courseId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "Course",
  },
  completedVideos: [
    {
      type: mongoose.Schema.Types.ObjectId,
      ref: "SubSection",
    },
  ],
});

module.exports = mongoose.model("CourseProgress", courseProgessSchema);

```

## backend\models\Invoices.js

```javascript

```

## backend\models\OTP.js

```javascript
const mongoose = require("mongoose");
const mailSender = require("../utils/mailSender");

const OTPSchema = mongoose.Schema({
    email:{
        type:String,
        required:true
    },
    otp:{
        type:String,
        required:true
    },
    createdAt:{
        type:Date,
        required:true,
        default:Date.now(),
        expires:5*60 //5 mins
    }
})


// Pre/post middlware
// User --> data enter --> otp-mail --> otp enter --> submit  --> db enter create (pre-save middlware)
// so otp model have mailsend nodemailer code

//function to send mail pre-middleware
async function sendVerificationEmail(email, otp) {
  try {
    const body = `<p>Your OTP for StudyNotion is <strong>${otp}</strong>. It is valid for 5 minutes.</p>`;
    const mailResponse = await mailSender(
      email,
      "Verification Email for StudyNotion",
      body,
    );
    console.log("Email sent successfully.", mailResponse);
  } catch (error) {
    console.log("Error while sending mail.", error);
    throw error;
  }
}

OTPSchema.pre("save", async function () {
  await sendVerificationEmail(this.email, this.otp);
});

module.exports = mongoose.model("OTP", OTPSchema);
```

## backend\models\Profile.js

```javascript
const mongoose = require("mongoose");

const profileSchema = new mongoose.Schema({
  gender: {
    type: String,
    enum: ["Male", "Female", "Other"],
    required: true,
  },
  dateOfBirth: {
    type: Date,
    required: true,
  },
  about: {
    type: String,
    trim: true,
  },
  contactNumber: {
    type: String,
    trim: true,
  },
});

module.exports = mongoose.model("Profile", profileSchema);

```

## backend\models\RatingAndReview.js

```javascript
const mongoose = require("mongoose");

const ratingAndReviewSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: "User",
    required: true,
  },
  rating: {
    type: Number,
    required: true,
  },
  review: {
    type: String,
    required: true,
  },
});

module.exports = mongoose.model("RatingAndReview", ratingAndReviewSchema);

```

## backend\models\Section.js

```javascript
const mongoose = require("mongoose");

const sectionSchema = new mongoose.Schema({
  sectionName: {
    type: String,
    required: true,
  },
  subSection: [
    {
      type: mongoose.Schema.Types.ObjectId,
      ref: "SubSection",
    },
  ],
});

module.exports = mongoose.model("Section", sectionSchema);

```

## backend\models\SubSection.js

```javascript
const mongoose = require("mongoose");

const subSectionSchema = new mongoose.Schema({
  title: {
    type: String,
    required: true,
    trim: true,
  },
  timeDuration: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    required: true,
  },
  videoUrl: {
    type: String,
    required: true,
  },
  additionalUrl: {
    type: String,
    required: false,
  },
});

module.exports = mongoose.model("SubSection", subSectionSchema);

```

## backend\models\User.js

```javascript
const mongoose = require("mongoose");
const { resetPasswordToken } = require("../controllers/ResetPassword");

const userSchema = new mongoose.Schema({
  firstName: {
    type: String,
    required: true,
    trim: true,
  },
  lastName: {
    type: String,
    required: true,
    trim: true,
  },
  email: {
    type: String,
    required: true,
  },
  password: {
    type: String,
    required: true,
  },
  accountType: {
    type: String,
    enum: ["Admin", "Student", "Instructor"],
    required: true,
  },
  additionalDetails: {
    type: mongoose.Schema.Types.ObjectId,
    required: true,
    ref: "Profile",
  },
  courses: [
    {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Course",
    },
  ],
  image: {
    type: String,
    require: true,
    default:
      "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_960_720.png",
  },
  courseProgess: [
    {
      type: mongoose.Schema.Types.ObjectId,
      ref: "CourseProgress",
    },
  ],
  //new added
  token:{
    type:String,
  },
  resetPasswordExpires:{
    type:Date
  }
});

module.exports = mongoose.model("User", userSchema);

```

## backend\package.json

```json
{
  "name": "backend",
  "version": "1.0.0",
  "description": "",
  "main": "index.js",
  "scripts": {
    "test": "echo \"Error: no test specified\" && exit 1",
    "start": "nodemon index.js",
    "dev": "nodemon index.js"
  },
  "keywords": [],
  "author": "",
  "license": "ISC",
  "dependencies": {
    "bcrypt": "^6.0.0",
    "cloudinary": "^2.9.0",
    "cookie-parser": "^1.4.7",
    "cors": "^2.8.6",
    "dotenv": "^17.2.3",
    "express": "^5.2.1",
    "jsonwebtoken": "^9.0.3",
    "mongoose": "^9.1.5",
    "nodemailer": "^7.0.13",
    "nodemon": "^3.1.11",
    "otp-generator": "^4.0.1",
    "razorpay": "^2.9.6"
  }
}

```

## backend\routes\Course.js

```javascript
// Import the required modules
const express = require("express")
const router = express.Router()

// Import the Controllers

// Course Controllers Import
const {
  createCourse,
  getAllCourses,
  getCourseDetails,
  getFullCourseDetails,
  editCourse,
  getInstructorCourses,
  deleteCourse,
} = require("../controllers/Course")


// Categories Controllers Import
const {
  showAllCategories,
  createCategory,
  categoryPageDetails,
} = require("../controllers/Category")

// Sections Controllers Import
const {
  createSection,
  updateSection,
  deleteSection,
} = require("../controllers/Section")

// Sub-Sections Controllers Import
const {
  createSubSection,
  updateSubSection,
  deleteSubSection,
} = require("../controllers/SubSection")

// Rating Controllers Import
const {
  createRating,
  getAverageRating,
  getAllRating,
} = require("../controllers/RatingAndReview")

const {
  updateCourseProgress
} = require("../controllers/courseProgress");

// Importing Middlewares
const { auth, isInstructor, isStudent, isAdmin } = require("../middlewares/auth")

// ********************************************************************************************************
//                                      Course routes
// ********************************************************************************************************

// Courses can Only be Created by Instructors
router.post("/createCourse", auth, isInstructor, createCourse)
//Add a Section to a Course
router.post("/addSection", auth, isInstructor, createSection)
// Update a Section
router.post("/updateSection", auth, isInstructor, updateSection)
// Delete a Section
router.post("/deleteSection", auth, isInstructor, deleteSection)
// Edit Sub Section
router.post("/updateSubSection", auth, isInstructor, updateSubSection)
// Delete Sub Section
router.post("/deleteSubSection", auth, isInstructor, deleteSubSection)
// Add a Sub Section to a Section
router.post("/addSubSection", auth, isInstructor, createSubSection)
// Get all Registered Courses
router.get("/getAllCourses", getAllCourses)
// Get Details for a Specific Courses
router.post("/getCourseDetails", getCourseDetails)
// Get Details for a Specific Courses
router.post("/getFullCourseDetails", auth, getFullCourseDetails)
// Edit Course routes
router.post("/editCourse", auth, isInstructor, editCourse)
// Get all Courses Under a Specific Instructor
router.get("/getInstructorCourses", auth, isInstructor, getInstructorCourses)
// Delete a Course
router.delete("/deleteCourse", auth, isInstructor, deleteCourse)

router.post("/updateCourseProgress", auth, isStudent, updateCourseProgress);

// ********************************************************************************************************
//                                      Category routes (Only by Admin)
// ********************************************************************************************************
// Category can Only be Created by Admin
// TODO: Put IsAdmin Middleware here
router.post("/createCategory", auth, isAdmin, createCategory)
router.get("/showAllCategories", showAllCategories)
router.post("/getCategoryPageDetails", categoryPageDetails)

// ********************************************************************************************************
//                                      Rating and Review
// ********************************************************************************************************
router.post("/createRating", auth, isStudent, createRating)
router.get("/getAverageRating", getAverageRating)
router.get("/getReviews", getAllRating)

module.exports = router
```

## backend\routes\Payments.js

```javascript
// Import the required modules
const express = require("express")
const router = express.Router()

const { capturePayment, verifyPayment, sendPaymentSuccessEmail } = require("../controllers/Payments")
const { auth, isInstructor, isStudent, isAdmin } = require("../middlewares/auth")
router.post("/capturePayment", auth, isStudent, capturePayment)
router.post("/verifyPayment",auth, isStudent, verifyPayment)
router.post("/sendPaymentSuccessEmail", auth, isStudent, sendPaymentSuccessEmail);

module.exports = router
```

## backend\routes\Profile.js

```javascript
const express = require("express")
const router = express.Router()
const { auth, isInstructor } = require("../middlewares/auth")
const {
  deleteAccount,
  updateProfile,
  getAllUserDetails,
  updateDisplayPicture,
  getEnrolledCourses,
  instructorDashboard,
} = require("../controllers/Profile")

// ********************************************************************************************************
//                                      Profile routes
// ********************************************************************************************************
// Delet User Account
router.delete("/deleteProfile", auth, deleteAccount)
router.put("/updateProfile", auth, updateProfile)
router.get("/getUserDetails", auth, getAllUserDetails)
// Get Enrolled Courses
router.get("/getEnrolledCourses", auth, getEnrolledCourses)
router.put("/updateDisplayPicture", auth, updateDisplayPicture)
router.get("/instructorDashboard", auth, isInstructor, instructorDashboard)

module.exports = router
```

## backend\routes\User.js

```javascript
// Import the required modules
const express = require("express")
const router = express.Router()

// Import the required controllers and middleware functions
const {
  login,
  signup,
  sendotp,
  changePassword,
} = require("../controllers/Auth")
const {
  resetPasswordToken,
  resetPassword,
} = require("../controllers/ResetPassword")

const { auth } = require("../middlewares/auth")

// Routes for Login, Signup, and Authentication

// ********************************************************************************************************
//                                      Authentication routes
// ********************************************************************************************************

// Route for user login
router.post("/login", login)

// Route for user signup
router.post("/signup", signup)

// Route for sending OTP to the user's email
router.post("/sendotp", sendotp)

// Route for Changing the password
router.post("/changepassword", auth, changePassword)

// ********************************************************************************************************
//                                      Reset Password
// ********************************************************************************************************

// Route for generating a reset password token
router.post("/reset-password-token", resetPasswordToken)

// Route for resetting user's password after verification
router.post("/reset-password", resetPassword)

// Export the router for use in the main application
module.exports = router
```

## backend\utils\imageUploader.js

```javascript
const cloudinary=require('cloudinary')

exports.uploadImageToCloudinary=async(file ,folder ,height,quality)=>{
    const options={folder};
    if(height){
        options.height=height;
    }
    if(quality){
        options.quality=quality;

    }
    options.resource_type="auto";
    return await cloudinary.uploader.upload(file.tempFilePath,options);
}
```

## backend\utils\mailSender.js

```javascript
const nodemailer = require("nodemailer");

const mailSender = async (email, subject, body) => {
  try {
    const transporter = nodemailer.createTransport({
      host: process.env.MAIL_HOST,
      auth: {
        user: process.env.MAIL_USER,
        pass: process.env.MAIL_PASSWORD,
      },
    });

    const info = await transporter.sendMail({
      from: "StudyNotion | By Aryan Akhare",
      to: email,
      subject,
      html: body,
    });

    return info;
  } catch (err) {
    console.error("Error sending mail", err);
    throw err;
  }
};

module.exports = mailSender;
```

## package.json

```json
{
  "dependencies": {
    "dotenv": "^17.3.1",
    "express-fileupload": "^1.5.2"
  }
}

```

