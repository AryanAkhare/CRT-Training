# StudyNotion Codex Context

This file is a handoff for a separate Codex chat or agent session.
It summarizes the current backend codebase, the execution flow, the main data model, and a step-by-step roadmap to recreate the app from scratch using Spring Boot, Maven, React, and MySQL.

## 1. Project Goal

Rebuild the StudyNotion application from scratch as a full-stack LMS web app:

- Backend: Spring Boot + Maven
- Database: MySQL
- Frontend: React
- IDE: IntelliJ IDEA

Primary learning goal:

- learn Spring Boot backend development through a real project
- replace the current Node/Mongo backend with a relational backend
- keep the frontend separate and consume REST APIs from Spring Boot

## 2. Current Repo Snapshot

Current structure in the repo:

- root contains helper notes and a `backend/` folder
- backend is a Node + Express + MongoDB implementation
- no React frontend code exists yet in this repo

Important root files:

- `progress.md`
- `progress.txt`
- `flow.txt`
- `http.txt`
- `todo.txt`

Important backend files:

- `backend/index.js`
- `backend/config/database.js`
- `backend/config/cloudinary.js`
- `backend/config/razorpay.js`
- `backend/middlewares/auth.js`
- `backend/routes/User.js`
- `backend/routes/Profile.js`
- `backend/routes/Course.js`
- `backend/routes/Payments.js`
- `backend/controllers/*`
- `backend/models/*`
- `backend/utils/*`
- `backend/mail/templates/*`

## 3. Current Backend Architecture

The backend is a REST API built with:

- Express
- Mongoose
- JWT auth
- bcrypt
- Nodemailer
- Cloudinary uploads
- Razorpay payments
- cookie-based token transport

### Startup flow

`backend/index.js`:

- loads environment variables
- connects to MongoDB
- connects to Cloudinary
- registers middleware:
  - `express.json()`
  - `cookie-parser`
  - `cors`
  - `express-fileupload`
- mounts routes:
  - `/api/v1/auth`
  - `/api/v1/profile`
  - `/api/v1/course`
  - `/api/v1/payments`
- exposes `/` health route
- returns 404 for unknown routes

### Important runtime dependencies in the current Node backend

- `express`
- `mongoose`
- `bcrypt`
- `jsonwebtoken`
- `cookie-parser`
- `cors`
- `nodemailer`
- `cloudinary`
- `razorpay`
- `express-fileupload`
- `otp-generator`
- `dotenv`

## 4. Backend File Inventory

### 4.1 Config

#### `backend/config/database.js`

- connects to MongoDB using `MONGODB_URL`
- logs success or exits on failure

#### `backend/config/cloudinary.js`

- configures Cloudinary with env vars:
  - `CLOUDINARY_CLOUD_NAME`
  - `CLOUDINARY_API_KEY`
  - `CLOUDINARY_API_SECRET`

#### `backend/config/razorpay.js`

- creates a Razorpay instance with:
  - `RAZORPAY_KEY`
  - `RAZORPAY_SECRET`

### 4.2 Middleware

#### `backend/middlewares/auth.js`

Roles and auth behavior:

- `auth`
  - reads token from cookie, request body, or `Authorization` header
  - verifies JWT with `JWT_SECRET`
  - attaches decoded payload to `req.user`
- `isStudent`
- `isAdmin`
- `isInstructor`

Current behavior:

- role checks are string comparisons on `req.user.accountType`
- failures return `401`

### 4.3 Utilities

#### `backend/utils/mailSender.js`

- sends HTML mail using Nodemailer
- uses env vars:
  - `MAIL_HOST`
  - `MAIL_USER`
  - `MAIL_PASSWORD`

#### `backend/utils/imageUploader.js`

- uploads files to Cloudinary
- expects `file.tempFilePath`
- supports folder, height, quality

### 4.4 Mail Templates

#### `backend/mail/templates/verificationEmail.js`

- OTP email HTML

#### `backend/mail/templates/resetPasswordEmail.js`

- password reset link email HTML

#### `backend/mail/templates/courseEnrollmentEmail.js`

- course enrollment confirmation email HTML

## 5. Data Model Summary

The current MongoDB models are already close to a relational LMS domain model.

### `User`

Key fields:

- `firstName`
- `lastName`
- `email`
- `password`
- `accountType` with values:
  - `Admin`
  - `Student`
  - `Instructor`
- `additionalDetails` ref to `Profile`
- `courses` ref array to `Course`
- `image`
- `courseProgess` ref array to `CourseProgress`
- `token`
- `resetPasswordExpires`

### `Profile`

Key fields:

- `gender`
- `dateOfBirth`
- `about`
- `contactNumber`

### `Course`

Key fields:

- `courseName`
- `courseDescription`
- `instructor` ref `User`
- `whatYouWillLearn`
- `courseContent` ref array to `Section`
- `ratingAndReviews` ref array to `RatingAndReview`
- `price`
- `tag`
- `category` ref `Category`
- `instructions`
- `status`
- `studentsEnrolled` ref array to `User`

### `Category`

Key fields:

- `name`
- `description`
- `course` ref `Course`

### `Section`

Key fields:

- `sectionName`
- `subSection` ref array to `SubSection`

### `SubSection`

Key fields:

- `title`
- `timeDuration`
- `description`
- `videoUrl`
- `additionalUrl`

### `RatingAndReview`

Key fields:

- `user` ref `User`
- `rating`
- `review`

### `OTP`

Key fields:

- `email`
- `otp`
- `createdAt`

Behavior:

- expires after 5 minutes via TTL
- pre-save hook sends email automatically

### `CourseProgress`

Key fields:

- `courseId` ref `Course`
- `completedVideos` ref array to `SubSection`

### `Invoices`

- file exists
- not actively used in the visible route/controller flow

## 6. Route Map

### `backend/routes/User.js`

Authentication and password routes:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/sendotp`
- `POST /api/v1/auth/changepassword`
- `POST /api/v1/auth/reset-password-token`
- `POST /api/v1/auth/reset-password`

### `backend/routes/Profile.js`

- `DELETE /api/v1/profile/deleteProfile`
- `PUT /api/v1/profile/updateProfile`
- `GET /api/v1/profile/getUserDetails`
- `GET /api/v1/profile/getEnrolledCourses`
- `PUT /api/v1/profile/updateDisplayPicture`
- `GET /api/v1/profile/instructorDashboard`

### `backend/routes/Course.js`

Course management routes:

- `POST /api/v1/course/createCourse`
- `POST /api/v1/course/addSection`
- `POST /api/v1/course/updateSection`
- `POST /api/v1/course/deleteSection`
- `POST /api/v1/course/updateSubSection`
- `POST /api/v1/course/deleteSubSection`
- `POST /api/v1/course/addSubSection`
- `GET /api/v1/course/getAllCourses`
- `POST /api/v1/course/getCourseDetails`
- `POST /api/v1/course/getFullCourseDetails`
- `POST /api/v1/course/editCourse`
- `GET /api/v1/course/getInstructorCourses`
- `DELETE /api/v1/course/deleteCourse`
- `POST /api/v1/course/updateCourseProgress`
- `POST /api/v1/course/createCategory`
- `GET /api/v1/course/showAllCategories`
- `POST /api/v1/course/getCategoryPageDetails`
- `POST /api/v1/course/createRating`
- `GET /api/v1/course/getAverageRating`
- `GET /api/v1/course/getReviews`

### `backend/routes/Payments.js`

- `POST /api/v1/payments/capturePayment`
- `POST /api/v1/payments/verifyPayment`
- `POST /api/v1/payments/sendPaymentSuccessEmail`

## 7. Controller Behavior Summary

### 7.1 `controllers/Auth.js`

#### `sendotp`

- validates `email`
- rejects if user already exists
- generates a unique 6-digit OTP
- stores OTP in `OTP`
- pre-save mail hook sends the OTP email

#### `signup`

- validates required fields:
  - `firstName`
  - `lastName`
  - `email`
  - `password`
  - `confirmPassword`
  - `otp`
- checks password confirmation
- checks existing user
- fetches most recent OTP for email
- validates OTP
- hashes password with bcrypt
- creates `Profile`
- creates `User`
- sets default avatar via dicebear

#### `login`

- validates `email` and `password`
- finds user and populates profile
- checks password with bcrypt
- issues JWT containing:
  - `email`
  - `id`
  - `accountType`
- stores token in `user.token`
- sends token as HTTP-only cookie

#### `changePassword`

- requires authenticated user
- validates old/new password
- verifies old password
- stores new hashed password

### 7.2 `controllers/ResetPassword.js`

#### `resetPasswordToken`

- validates user email
- finds user
- creates random reset token
- stores token and expiry on user
- builds frontend reset URL
- sends reset email

#### `resetPassword`

- validates `password`, `confirmPassword`, `token`
- checks password match
- validates token existence
- checks expiry
- hashes new password
- clears token and expiry

### 7.3 `controllers/Profile.js`

#### `updateProfile`

- uses authenticated user id
- updates profile fields:
  - `dateOfBirth`
  - `about`
  - `gender`
  - `contactNumber`

#### `deleteAccount`

- deletes associated `Profile`
- deletes `User`
- current code comments mention future cleanup for enrollments

#### `getAllUserDetails`

- returns authenticated user
- populates `additionalDetails`

#### `updateDisplayPicture`

- uploads image to Cloudinary
- updates `User.image`

#### `getEnrolledCourses`

- populates user `courses`
- populates instructor name and category

#### `instructorDashboard`

- loads all courses taught by instructor
- counts total courses and total enrolled students

### 7.4 `controllers/Course.js`

#### `createCourse`

- validates:
  - `courseName`
  - `courseDescription`
  - `whatYouWillLearn`
  - `price`
  - category
  - thumbnail file
- checks authenticated instructor
- validates category
- uploads thumbnail to Cloudinary
- creates `Course`
- pushes course id to instructor `courses`
- pushes course id to category `courses`

#### `getAllCourses`

- returns all course summary records
- populates instructor

#### `getCourseDetails`

- fetches course by id
- populates:
  - instructor
  - category
  - ratingAndReviews
  - courseContent -> subSection

#### `getFullCourseDetails`

- same general idea as `getCourseDetails`
- more detailed review population

#### `editCourse`

- validates course ownership
- updates course fields selectively

#### `getInstructorCourses`

- returns all courses owned by authenticated instructor

#### `deleteCourse`

- validates ownership
- deletes course

### 7.5 `controllers/Section.js`

#### `createSection`

- creates section
- pushes section id into course `courseContent`
- populates subsections afterward

#### `updateSection`

- updates section name

#### `deleteSection`

- removes section ref from course
- deletes section document

### 7.6 `controllers/SubSection.js`

#### `createSubSection`

- validates all fields
- uploads video to Cloudinary
- creates subsection
- pushes subsection id into section `subSection`

#### `updateSubSection`

- selectively updates subsection fields
- optionally reuploads video

#### `deleteSubSection`

- removes subsection ref from section
- deletes subsection document

### 7.7 `controllers/Category.js`

#### `createCategory`

- admin-only in route
- validates `name` and `description`
- rejects duplicate category names

#### `showAllCategories`

- returns all categories

#### `categoryPageDetails`

- validates category id
- loads selected category
- loads other categories
- loads top-selling courses

### 7.8 `controllers/RatingAndReview.js`

#### `createRating`

- checks user enrolled in course
- checks if already reviewed
- creates rating/review
- appends review id to course

#### `getAverageRating`

- aggregates average rating for a course

#### `getAllRating`

- fetches all reviews
- sorts descending by rating
- populates user and course

### 7.9 `controllers/Payments.js`

#### `capturePayment`

- validates authenticated user and course
- checks existing enrollment
- creates Razorpay order
- returns order metadata to frontend

#### `verifySignature`

- verifies Razorpay webhook signature
- enrolls student in course
- adds course to user
- sends enrollment email

#### `sendPaymentSuccessEmail`

- idempotent email helper for successful payment

### 7.10 `controllers/courseProgress.js`

#### `updateCourseProgress`

- validates user, course, subsection
- creates or updates progress record
- stores completed video ids

## 8. Known Issues and Gaps in the Current Backend

These are important if the next chat is going to rebuild the app cleanly.

- data model is MongoDB-specific and needs normalization for MySQL
- several controller functions still have weak validation
- some status codes are inconsistent
- some route names are mixed case or inconsistent
- some fields are named inconsistently:
  - `courseProgess`
  - `studentEnrolled` vs `studentsEnrolled`
  - `tag` vs `category`
- some controllers have logic bugs or copy-paste mistakes
  - example: review duplicate-check uses the wrong variable in one branch
- `Invoices` model is present but not clearly wired into routes
- there are TODO comments for:
  - cleanup on account delete
  - better HTTP status codes
  - better security
  - OAuth
  - UPI payment improvements
  - readable inline comments
  - stronger flow documentation

## 9. Recommended Rebuild Strategy

If rebuilding from scratch, do not port file-for-file first.
Use a domain-first rewrite.

### Recommended stack

- Backend: Spring Boot
- Build tool: Maven
- Database: MySQL
- Frontend: React
- API style: REST
- Auth: JWT
- Storage: Cloudinary or S3 for media
- Email: Spring Mail
- Payments: Razorpay

### Why MySQL fits better here

The app is relational:

- users enroll in courses
- courses have sections
- sections have subsections
- reviews belong to users and courses
- progress belongs to users and courses
- payments and invoices are transactional

This is a better match for MySQL + JPA than for a document database.

## 10. Step-by-Step Roadmap for the New Spring Boot + React App

### Phase 1: Project bootstrap

1. Create a Spring Boot Maven project in IntelliJ
2. Create a separate React project with Vite
3. Set up MySQL locally
4. Create database schema name, for example `studynotion`
5. Configure Spring Boot datasource
6. Verify backend starts cleanly
7. Verify frontend starts cleanly

### Phase 2: Domain design

1. Define all entities
2. Define relations
3. Define enums for role and course status
4. Decide what is one-to-one, one-to-many, and many-to-many
5. Write DTOs before writing controllers

Suggested entities:

- `User`
- `Profile`
- `Otp`
- `Course`
- `Category`
- `Section`
- `Lecture` or `SubSection`
- `Enrollment`
- `CourseProgress`
- `Review`
- `PaymentOrder`
- `Invoice`

### Phase 3: Security and auth

1. Configure Spring Security
2. Add JWT generation and validation
3. Add password hashing
4. Add role-based access control
5. Implement signup
6. Implement login
7. Implement email OTP verification
8. Implement forgot password
9. Implement reset password
10. Implement change password

### Phase 4: User/profile APIs

1. Get current user
2. Update profile
3. Upload profile image
4. Delete account
5. Fetch enrolled courses
6. Build instructor dashboard summary

### Phase 5: Course APIs

1. Create category
2. List categories
3. Create course
4. Edit course
5. Delete course
6. List all published courses
7. Fetch course details
8. Fetch full course details
9. Add section
10. Edit section
11. Delete section
12. Add lecture/subsection
13. Edit lecture/subsection
14. Delete lecture/subsection

### Phase 6: Learning flow

1. Enroll student in course
2. Track progress per lecture
3. Mark lecture complete
4. Get progress percentage
5. Build student dashboard views

### Phase 7: Reviews and ratings

1. Allow only enrolled students to review
2. Prevent duplicate reviews
3. Compute average rating
4. List all reviews

### Phase 8: Payments

1. Create payment order
2. Verify payment
3. Store payment record
4. Enroll user on successful payment
5. Send enrollment confirmation email

### Phase 9: File uploads

1. Integrate Cloudinary or S3
2. Upload course thumbnail
3. Upload lecture media
4. Upload profile images
5. Abstract upload logic into a service

### Phase 10: React frontend

Build pages in this order:

1. Home
2. Login
3. Signup
4. Forgot password
5. Reset password
6. Course catalog
7. Course details
8. Checkout/payment
9. Student dashboard
10. Instructor dashboard
11. Admin category management
12. Profile/settings

Frontend architecture:

- `pages`
- `components`
- `routes`
- `services`
- `hooks`
- `context` or `store`
- `utils`

### Phase 11: Integration and testing

1. Test APIs with Postman
2. Add backend unit tests
3. Add backend integration tests
4. Add frontend form validation
5. Add protected routes
6. Verify cookie/JWT handling
7. Add Swagger/OpenAPI

### Phase 12: Deployment

1. Deploy backend
2. Deploy MySQL
3. Deploy frontend
4. Configure production env vars
5. Test CORS and auth in production

## 11. Suggested Spring Boot Package Structure

```text
com.studynotion
  config
  controller
  dto
  entity
  exception
  mapper
  repository
  security
  service
  util
```

## 12. Suggested First Implementation Order

If starting today, code in this exact order:

1. Spring Boot project creation
2. MySQL connection
3. `User` and `Profile` entities
4. JWT security setup
5. Signup/login APIs
6. OTP email flow
7. Reset password flow
8. Course/category entities
9. Section and lecture entities
10. Enrollment and progress
11. Reviews
12. Payments
13. React frontend

## 13. Suggested IntelliJ Setup

When creating the backend project in IntelliJ:

- choose Spring Initializr
- use Maven
- use Java 17 or 21
- enable Lombok annotation processing
- keep backend and frontend as separate projects

Suggested backend dependencies:

- Spring Web
- Spring Security
- Spring Data JPA
- Validation
- MySQL Driver
- Lombok
- Spring Boot DevTools
- Spring Mail
- Spring Boot Test
- later add JWT and Swagger dependencies

## 14. Notes for the Next Codex Chat

If another Codex chat uses this file, it should:

- treat the current Node backend as reference only
- rebuild the app in Spring Boot rather than patching the Node code
- use MySQL, not MongoDB
- keep frontend and backend separate
- implement auth and security before course features
- prioritize domain design before writing controllers
- validate every API request with DTOs and annotations
- use proper HTTP status codes and centralized exception handling

## 15. Short Summary

This repo currently contains a Node + MongoDB LMS backend with:

- JWT login/signup
- OTP registration
- password reset
- profile management
- course/category/section/subsection management
- Cloudinary uploads
- ratings and reviews
- Razorpay payment flow
- email notifications

The correct scratch rebuild path is:

- Spring Boot + Maven backend
- MySQL database
- React frontend
- JWT security
- REST APIs
- Cloudinary or S3 for media
- Razorpay for payments

