package dev.langchain4j.model.image;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.output.Response;
import java.util.List;

public interface ImageModel {

    Response<Image> generate(String prompt);
}
