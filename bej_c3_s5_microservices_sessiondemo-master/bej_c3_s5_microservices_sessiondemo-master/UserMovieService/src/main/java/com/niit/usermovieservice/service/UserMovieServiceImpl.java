package com.niit.usermovieservice.service;

import com.niit.usermovieservice.domain.Movie;
import com.niit.usermovieservice.domain.User;
import com.niit.usermovieservice.exception.MovieNotFoundException;
import com.niit.usermovieservice.exception.UserAlreadyExistsException;
import com.niit.usermovieservice.exception.UserNotFoundException;
import com.niit.usermovieservice.repository.UserMovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class UserMovieServiceImpl implements UserMovieService{
    private UserMovieRepository userMovieRepository;
    @Autowired
    public UserMovieServiceImpl(UserMovieRepository userMovieRepository) {
        this.userMovieRepository = userMovieRepository;
    }

    @Override
    public User registerUser(User user) throws UserAlreadyExistsException {
        if(userMovieRepository.findById(user.getEmail()).isPresent())
        {
            throw new UserAlreadyExistsException();
        }
        return userMovieRepository.save(user);
    }

    @Override
    public User saveUserMovieToList(Movie movie, String email) throws UserNotFoundException {
        User user = userMovieRepository.findById(email).orElseThrow(UserNotFoundException::new);
        List<Movie> movies = user.getMovieList() == null ? new ArrayList<>() : new ArrayList<>(user.getMovieList());
        movies.add(movie);
        user.setMovieList(movies);
        return userMovieRepository.save(user);
    }

    @Override
    public User deleteUserMovieFromList(String email, String movieId) throws UserNotFoundException, MovieNotFoundException {
        User user = userMovieRepository.findById(email).orElseThrow(UserNotFoundException::new);
        List<Movie> movies = user.getMovieList();
        if (movies == null || movies.isEmpty()) {
            throw new MovieNotFoundException();
        }

        List<Movie> updatedMovies = new ArrayList<>(movies);
        boolean movieIdIsPresent = updatedMovies.removeIf(x -> movieId.equals(x.getMovieId()));
        if (!movieIdIsPresent) {
            throw new MovieNotFoundException();
        }

        user.setMovieList(updatedMovies);
        return userMovieRepository.save(user);
    }

    @Override
    public List<Movie> getAllUserMovies(String email) throws UserNotFoundException {
        User user = userMovieRepository.findById(email).orElseThrow(UserNotFoundException::new);
        return user.getMovieList() == null ? new ArrayList<>() : new ArrayList<>(user.getMovieList());
    }

    @Override
    public List<Movie> getAllMoviesByGenre(String genre) {
      return userMovieRepository.findAll().stream()
              .filter(user -> user.getMovieList() != null)
              .flatMap(user -> user.getMovieList().stream())
              .filter(movie -> genre == null || genre.equalsIgnoreCase(movie.getGenre()))
              .collect(Collectors.toList());
    }

    @Override
    public List<Movie> getAllMovies() {
        return userMovieRepository.findAll().stream()
                .filter(user -> user.getMovieList() != null)
                .flatMap(user -> user.getMovieList().stream())
                .collect(Collectors.toList());
    }
}
