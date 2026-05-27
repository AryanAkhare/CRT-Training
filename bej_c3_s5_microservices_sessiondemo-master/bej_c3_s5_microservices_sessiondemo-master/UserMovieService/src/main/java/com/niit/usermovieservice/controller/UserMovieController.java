package com.niit.usermovieservice.controller;

import com.niit.usermovieservice.domain.Movie;
import com.niit.usermovieservice.domain.User;
import com.niit.usermovieservice.exception.MovieNotFoundException;
import com.niit.usermovieservice.exception.UserAlreadyExistsException;
import com.niit.usermovieservice.exception.UserNotFoundException;
import com.niit.usermovieservice.service.UserMovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/")
public class UserMovieController {
    private UserMovieService userMovieService;

    @Autowired
    public UserMovieController(UserMovieService userMovieService) {
        this.userMovieService = userMovieService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) throws UserAlreadyExistsException {
        return new ResponseEntity<>(userMovieService.registerUser(user), HttpStatus.CREATED);
    }
    @PostMapping("/user/{email}/movie")
    public ResponseEntity<?> saveUserMovieToList(@RequestBody Movie movie, @PathVariable String email) throws UserNotFoundException {
        return new ResponseEntity<>(userMovieService.saveUserMovieToList(movie, email), HttpStatus.CREATED);
    }
    @GetMapping("/user/{email}/movies")
    public ResponseEntity<?> getAllUserMoviesFromList(@PathVariable String email) throws UserNotFoundException {
       return new ResponseEntity<>(userMovieService.getAllUserMovies(email), HttpStatus.OK);
    }
    @DeleteMapping("/user/{email}/{movieId}")
    public ResponseEntity<?> deleteUserProductFromList(@PathVariable String email,@PathVariable String movieId)
            throws UserNotFoundException, MovieNotFoundException
    {
        return new ResponseEntity<>(userMovieService.deleteUserMovieFromList(email, movieId), HttpStatus.OK);
    }
}
