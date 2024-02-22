package hei.school.sarisary.service;

import hei.school.sarisary.file.BucketComponent;
import hei.school.sarisary.file.FileHash;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ImageService {
  private final BucketComponent bucketComponent;

  public byte[] makeGray(BufferedImage img) {
    ByteArrayOutputStream byteImage = new ByteArrayOutputStream();
    try {
      for (int x = 0; x < img.getWidth(); ++x) {
        for (int y = 0; y < img.getHeight(); ++y) {
          int rgb = img.getRGB(x, y);
          int r = (rgb >> 16) & 0xFF;
          int g = (rgb >> 8) & 0xFF;
          int b = (rgb & 0xFF);
          double color =
              0.2126 * Math.pow(r / 255.0, 2.2)
                  + 0.7152 * Math.pow(g / 255.0, 2.2)
                  + 0.0722 * Math.pow(b / 255.0, 2.2);
          int contrast = (int) (255.0 * Math.pow(color, 1.0 / 2.2));
          int grey = (contrast << 16) + (contrast << 8) + contrast;
          img.setRGB(x, y, grey);
        }
      }
      ImageIO.write(img, "jpeg", byteImage);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return byteImage.toByteArray();
  }

  public String uploadToS3(BufferedImage originalImage, String bucketKey) throws IOException {
    byte[] grayImageData = makeGray(originalImage);

    File originalFile = File.createTempFile("originalImage", ".jpeg");
    ImageIO.write(originalImage, "jpeg", originalFile);
    bucketComponent.upload(originalFile, bucketKey + "/original");
    try {
      FileHash grayFileHash = uploadFileToS3(grayImageData, bucketKey + "/grey");
      return "file added successfully : " + grayFileHash;
    } catch (Exception e) {
      return "error" + e;
    }
  }

  private FileHash uploadFileToS3(byte[] fileData, String bucketKey) {
    try {
      File tempFile = File.createTempFile("tempImage", ".jpeg");
      try (FileOutputStream fos = new FileOutputStream(tempFile)) {
        fos.write(fileData);
      }
      return bucketComponent.upload(tempFile, bucketKey);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ResponseEntity<Map<String, String>> getPresignedImageUrls(
      String idOriginal, String idGray) {
    Map<String, String> responseMap = new HashMap<>();
    responseMap.put("original_url", originalImageUrl(idOriginal).toString());
    responseMap.put("transformed_url", grayImageUrl(idGray).toString());

    return new ResponseEntity<>(responseMap, HttpStatus.OK);
  }

  private URL originalImageUrl(String id) {
    return bucketComponent.presign(id, Duration.ofDays(3));
  }

  private URL grayImageUrl(String id) {
    return bucketComponent.presign(id, Duration.ofDays(3));
  }
}
