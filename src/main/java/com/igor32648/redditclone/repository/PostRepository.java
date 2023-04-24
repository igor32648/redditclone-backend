package com.igor32648.redditclone.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.igor32648.redditclone.model.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

}
