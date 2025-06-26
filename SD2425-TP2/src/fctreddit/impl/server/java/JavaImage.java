package fctreddit.impl.server.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import fctreddit.api.User;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.java.Result.ErrorCode;
import fctreddit.api.kafka.RecordProcessor;
import fctreddit.api.rest.RestImage;
import fctreddit.impl.kafka.KafkaPublisher;
import fctreddit.impl.kafka.KafkaSubscriber;
import fctreddit.impl.kafka.KafkaUtils;
import fctreddit.impl.server.grpc.ImageServer;
import fctreddit.impl.server.rest.ImageResource;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class JavaImage extends JavaServer implements Image, RecordProcessor {

	private static Logger Log = Logger.getLogger(JavaImage.class.getName());
	
	private static final Path baseDirectory = Path.of("home", "sd", "images");

	private static final long REFERENCE_TIMEOUT = 30000;
	private static final long CLEANUP_SLEEP = 1000;

	private static final String TOPIC_CONTENT_DELETED_IMAGE = "content-deleted-image";
	private static final String TOPIC_IMAGE_REFERENCED = "image-referenced";
	private static final String TOPIC_IMAGE_UNREFERENCED = "image-not-referenced";
	private static final String TOPIC_IMAGE_DELETED_USER = "image-deleted-user";
	private static volatile boolean init = false;
	private static Map<String, ImageInfo> images;

	private static KafkaSubscriber subscriber = null;
	private static KafkaPublisher publisher = null;

	private static class ImageInfo {
		String userId;
		long timestamp;
		int referenceCount;

		ImageInfo(String userId, long timestamp, int referenceCount) {
			this.userId = userId;
			this.timestamp = timestamp;
			this.referenceCount = referenceCount;
		}
	}

	private void initKafka(){
		Log.info("\n\n\t Starting kafka topic creation...\n\n\n");
		KafkaUtils.createTopic(TOPIC_CONTENT_DELETED_IMAGE);
		KafkaUtils.createTopic(TOPIC_IMAGE_REFERENCED);
		KafkaUtils.createTopic(TOPIC_IMAGE_UNREFERENCED);
		KafkaUtils.createTopic(TOPIC_IMAGE_DELETED_USER);
		if(subscriber == null)
			subscriber = KafkaSubscriber.createSubscriber("kafka:9092", List.of(TOPIC_IMAGE_REFERENCED, TOPIC_IMAGE_UNREFERENCED));
		if(publisher == null)
			publisher = KafkaPublisher.createPublisher("kafka:9092");

		subscriber.start(this);
	}

	public void startAsync(){
		if (!init) {
			synchronized (JavaImage.class) {
				if (!init) { // double-checked locking
					initKafka();
					iterateImages();
					init = true;
				}
			}
		}
	}

	public JavaImage() {
		File f = baseDirectory.toFile();

		if (!f.exists()) {
			f.mkdirs();
		}

		if(images == null)
			images = new ConcurrentHashMap<>();

		startAsync();
	}

	@Override
	public Result<String> createImage(String userId, byte[] imageContents, String password) throws Exception {

		Result<User> owner = getUsersClient().getUser(userId, password);

		if (!owner.isOK())
			return Result.error(owner.error());

		String id = null;
		Path image = null;

		// check if user directory exists
		Path userDirectory = Path.of(baseDirectory.toString(), userId);
		File uDir = userDirectory.toFile();
		if (!uDir.exists()) {
			uDir.mkdirs();
		}

		synchronized (this) {
			while (true) {
				id = UUID.randomUUID().toString();
				image = Path.of(userDirectory.toString(), id);
				File iFile = image.toFile();

				if (!iFile.exists())
					break;
			}

			try {
				Files.write(image, imageContents);
				images.put(id, new ImageInfo(userId, System.currentTimeMillis(), 0) );
			} catch (IOException e) {
				e.printStackTrace();
				return Result.error(ErrorCode.INTERNAL_ERROR);
			}
		}
		
		Log.info("Created image with id " + id + " for user " + userId);
		
		return Result.ok(id);
	}

	@Override
	public Result<byte[]> getImage(String userId, String imageId) {
		Log.info("Get image with id " + imageId + " owned by user " + userId);
		
		Path image = Path.of(baseDirectory.toString(), userId, imageId);
		File iFile = image.toFile();

		synchronized (this) {
			if (iFile.exists() && iFile.isFile()) {
				try {
					Log.info("Fetching image " + imageId);
					return Result.ok(Files.readAllBytes(image));
				} catch (IOException e) {
					e.printStackTrace();
					return Result.error(ErrorCode.INTERNAL_ERROR);
				}
			} else {
				Log.info("Image " + imageId + " not found");
				return Result.error(ErrorCode.NOT_FOUND);
			}
		}
		
	}

	@Override
	public Result<Void> deleteImage(String userId, String imageId, String password) throws Exception {
		Log.info("Delete image with id " + imageId + " owned by user " + userId);
		
		Result<User> owner = getUsersClient().getUser(userId, password);
		if (!owner.isOK()) {
			Log.info("Failed to authenticate user: " + owner.error());
			return Result.error(owner.error());
		}

		Path image = Path.of(baseDirectory.toString(), userId, imageId);
		File iFile = image.toFile();

		synchronized (this) {
			if (iFile.exists() && iFile.isFile()) {
				iFile.delete();

				String imagePath = ImageResource.baseURI + RestImage.PATH + "/" + userId + "/" + imageId;

				publisher.publish(TOPIC_CONTENT_DELETED_IMAGE, imagePath);
				images.remove(imageId);
				return Result.ok();
			} else {
				return Result.error(ErrorCode.NOT_FOUND);
			}
		}
	}

	private void internalDeleteImage(String userId, String imageId, String topic) {
		Log.info("Delete image with id " + imageId + " owned by user " + userId);

		Path image = Path.of(baseDirectory.toString(), userId, imageId);
		File iFile = image.toFile();

		synchronized (this) {
			if (iFile.exists() && iFile.isFile()) {
				iFile.delete();
				images.remove(imageId);
				if(topic.equals(TOPIC_IMAGE_DELETED_USER)){
					String imagePath = ImageResource.baseURI + RestImage.PATH + "/" + userId + "/" + imageId;
					publisher.publish(TOPIC_CONTENT_DELETED_IMAGE, imagePath);
				}
				Log.info("Image successfully deleted: " + imageId);
			} else {
				Log.info("Error deleting image: " + imageId);
			}
		}
	}

	private void iterateImages() {
		new Thread(() -> {
			for(;;){
				try {
					for (Map.Entry<String, ImageInfo> entry : images.entrySet()) {
						String imageId = entry.getKey();
						String userId = entry.getValue().userId;
						long timestamp = entry.getValue().timestamp;
						long refenceCount = entry.getValue().referenceCount;
						long now = System.currentTimeMillis();
						if (refenceCount == 0 && now - timestamp >= REFERENCE_TIMEOUT) {
							internalDeleteImage(userId, imageId, null);
						}
					}

					Thread.sleep(CLEANUP_SLEEP);
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}


	@Override
	public void onReceive(ConsumerRecord<String, String> r) {
		System.out.println("\n\n\t" + r.topic() + " , " +  r.offset() + " -> " + r.value());

		if(r.topic().equals(TOPIC_IMAGE_REFERENCED) && images.containsKey(r.value())) {
			images.get(r.value()).referenceCount++;
			Log.info("ADDED REFERENCE TO IMAGE: " + r.value() + " " + images.get(r.value()).referenceCount);
		}
		if(r.topic().equals(TOPIC_IMAGE_UNREFERENCED) && images.containsKey(r.value())) {
			images.get(r.value()).referenceCount--;
			Log.info("REMOVED REFERENCE TO IMAGE: " + r.value() + " " + images.get(r.value()).referenceCount);
		}
		if(r.topic().equals(TOPIC_IMAGE_DELETED_USER) && images.containsKey(r.value())) {
			internalDeleteImage(images.get(r.value()).userId, r.value(), TOPIC_IMAGE_DELETED_USER);
		}
	}
}
