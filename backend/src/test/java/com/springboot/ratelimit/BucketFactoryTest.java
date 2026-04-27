package com.springboot.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

class BucketFactoryTest {

    @Test
    void testLocalBucketAllowsWithinCapacity() {
        BucketFactory factory = new BucketFactory();
        Bucket bucket = factory.getLocalBucket("test-key", 5, 5, 60);
        for (int i = 0; i < 5; i++) {
            assertThat(bucket.tryConsume(1)).isTrue();
        }
    }

    @Test
    void testLocalBucketDeniesOverCapacity() {
        BucketFactory factory = new BucketFactory();
        Bucket bucket = factory.getLocalBucket("test-key-2", 3, 3, 60);
        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
    }

    @Test
    void testLocalBucketRefillsOverTime() throws InterruptedException {
        BucketFactory factory = new BucketFactory();
        Bucket bucket = factory.getLocalBucket("test-key-3", 2, 2, 1);
        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isTrue();
        assertThat(bucket.tryConsume(1)).isFalse();
        Thread.sleep(1100);
        assertThat(bucket.tryConsume(1)).isTrue();
    }

    @Test
    void testDifferentKeysHaveIndependentBuckets() {
        BucketFactory factory = new BucketFactory();
        Bucket bucket1 = factory.getLocalBucket("key-a", 2, 2, 60);
        Bucket bucket2 = factory.getLocalBucket("key-b", 2, 2, 60);
        assertThat(bucket1.tryConsume(2)).isTrue();
        assertThat(bucket2.tryConsume(2)).isTrue();
        assertThat(bucket1.tryConsume(1)).isFalse();
        assertThat(bucket2.tryConsume(1)).isFalse();
    }

    @Test
    void testDistributedBucketThrowsWhenNotEnabled() {
        BucketFactory factory = new BucketFactory();
        assertThat(factory.isDistributedEnabled()).isFalse();
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> factory.getDistributedBucket("test", 10, 10, 1));
    }
}
