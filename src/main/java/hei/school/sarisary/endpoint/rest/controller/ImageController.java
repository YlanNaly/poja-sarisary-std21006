package hei.school.sarisary.endpoint.rest.controller;

import hei.school.sarisary.service.ImageService;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
public class ImageController {
  private final ImageService service;

  @PutMapping("/black-and-white/{id}")
  public String uploadImageToGray(@PathVariable String id, @RequestBody BufferedImage image)
      throws IOException {
    return service.uploadToS3(image, id);
  }

  @GetMapping("/black-and-white/{id}")
  public ResponseEntity<Map<String, String>> getImage(@PathVariable String id) {
    String idOriginal = id + "/original";
    String idGrey = id + "/grey";

    return service.getPresignedImageUrls(idOriginal, idGrey);
  }
}
