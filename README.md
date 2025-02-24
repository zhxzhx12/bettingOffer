Design Considerations:
1. As required, all data is stored in memory, and some data (customer stake history) needs to be retained indefinitely, so memory will gradually be exhausted over time. Therefore, attention needs to be paid to the efficiency of data storage.
2. Use primitive data type `int` instead of `Integer` to store customer stake history. The specific structure is `Map<Integer-BetOfferId, Map<Integer-customerId, int[Stake]>>`.
3. Use `ArrayList` rather than `Map` to store sessions, keeping an index within the session key for faster searching.
4. Enable a background process to periodically clean up expired data to reduce memory usage: A: Expired sessions. B: High stakes outside the top 20 ranks.
5. Use `VirtualThread` to handle requests received in `httpserve` to improve system processing capacity.
6. Use relevant tools from `java.util.concurrent` (JUC) for synchronization in threads to reduce the use of heavyweight locks.
7. Based on java21.

Other Matters:
1. Use `maven-assembly-plugin` to assist in packaging runnable jar files.
2. Use `google jib-maven-plugin` to assist in packaging Docker images.
3. Use `Testcontainers` and `JUnit` for integration testing.

