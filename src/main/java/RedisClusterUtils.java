import com.hyr.redis.help.RedisHelper;
import com.hyr.redis.message.ResultMessage;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.*;
import redis.clients.util.JedisClusterCRC16;

import java.text.SimpleDateFormat;
import java.util.*;

/*******************************************************************************
 * @date 2018-02-28 下午 5:45
 * @author: <a href=mailto:huangyr@bonree.com>黄跃然</a>
 * @Description: 集群环境redis操作指令
 ******************************************************************************/
public class RedisClusterUtils {

    private static RedisClusterProxy jedisCluster = null;

    private RedisClusterUtils(RedisClusterProxy jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    /**
     * get redis cluster instance, the instance is single
     *
     * @param hostAndPortAddress split by ','
     * @return
     */
    static RedisClusterProxy getRedisClusterInstance(String hostAndPortAddress) {
        if (jedisCluster == null) {
            if (StringUtils.isEmpty(hostAndPortAddress)) {
                return null;
            }
            Set<HostAndPort> hostAndPorts = getHostAndPort(hostAndPortAddress);
            jedisCluster = new RedisClusterProxy(hostAndPorts);
        }
        return jedisCluster;
    }

    /**
     * @param hostAndPortAddress
     * @return
     */
    private static Set<HostAndPort> getHostAndPort(String hostAndPortAddress) {
        Set<HostAndPort> hostAndPorts = new HashSet<HostAndPort>();
        String[] hostAndPortArray = hostAndPortAddress.split(",");
        for (String hostAndPort : hostAndPortArray) {
            String[] hostWithPort = hostAndPort.split(":");
            hostAndPorts.add(new HostAndPort(hostWithPort[0], Integer.parseInt(hostWithPort[1])));
        }
        return hostAndPorts;
    }


    /**
     * command : keys
     *
     * @param jedisCluster
     * @param pattern
     * @return
     */
    static TreeSet<String> keys(RedisClusterProxy jedisCluster, String pattern) {
        TreeSet<String> keys = new TreeSet<String>();
        Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
        for (String node : clusterNodes.keySet()) {
            JedisPool jedisPool = clusterNodes.get(node);
            if (jedisPool != null) {
                Jedis jedis = jedisPool.getResource();
                try {
                    keys.addAll(jedis.keys(pattern));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    jedis.close();//用完一定要close这个链接！！！
                }
            }
        }
        return keys;
    }

    /**
     * command : cluster info
     *
     * @param jedisCluster
     * @return
     */
    static String info(RedisClusterProxy jedisCluster) {
        Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
        StringBuilder sb = new StringBuilder();
        for (String node : clusterNodes.keySet()) {
            JedisPool jedisPool = clusterNodes.get(node);
            if (jedisPool != null) {
                Jedis jedis = jedisPool.getResource();
                try {
                    String info = jedis.info();
                    sb.append(info).append("=====================================================\n").append("\n");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    jedis.close();
                }
            }
        }
        return sb.toString();
    }

    /**
     * command : cluster nodes
     *
     * @param jedisCluster
     * @return
     */
    static String nodes(RedisClusterProxy jedisCluster) {
        Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
        StringBuilder sb = new StringBuilder();
        for (String node : clusterNodes.keySet()) {
            JedisPool jedisPool = clusterNodes.get(node);
            if (jedisPool != null) {
                Jedis jedis = jedisPool.getResource();
                try {
                    String info = jedis.clusterNodes();
                    sb.append(info).append("=====================================================\n").append("\n");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    jedis.close();//用完一定要close这个链接！！！
                }
            }
        }
        return sb.toString();
    }

    /**
     * command : call
     *
     * @param jedisCluster
     * @return
     */
    static String call(RedisClusterProxy jedisCluster, String script) {
        try {
            JedisSlotBasedConnectionHandlerProxy connectionHandler = jedisCluster.getConnectionHandler();
            Jedis redis = connectionHandler.getConnection();
            Object result = redis.eval(script);
            if (result != null && result instanceof List) {
                List list = (List) result;
                return (String) list.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//    /**
//     * command : multi exec
//     *
//     * @param jedisCluster
//     * @return
//     */
//    static boolean transaction(RedisClusterProxy jedisCluster, RedisCallBack redisCallBack) {
//        boolean result = true;
//        Jedis redis = null;
//        try {
//            JedisSlotBasedConnectionHandlerProxy connectionHandler = jedisCluster.getConnectionHandler();
//
//            redis = connectionHandler.getConnection();
//
//            // TODO 改为可以识别散列方法
//            // TODO 可以优化为动态代理
//            result = redisCallBack.MultiAndExec(redis);
//
//        } catch (Exception e) {
//            result = false;
//            e.printStackTrace();
//        } finally {
//            if (redis != null) {
//                redis.close();
//            }
//        }
//        return result;
//    }
//
//    abstract static class RedisCallBack {
//
//        boolean MultiAndExec(Jedis redis) {
//            boolean result = true;
//            try {
//                List<String> keys = setKey();
//                //allotSlot(redis, keys);
//                Pipeline pipelined = redis.pipelined();
//                pipelined.multi();
//                pipelined.set("a7", "c4");
//                pipelined.set("a8", "c5");
//                pipelined.set("a9", "c6");
//                //OnMultiAndExecListener(redis.multi());
//                pipelined.exec();
//                pipelined.sync();
//            } catch (Exception e) {
//                result = false;
//                e.printStackTrace();
//            }
//            return result;
//        }
//
//        /**
//         * allot slot by key
//         *
//         * @param redis
//         * @param keys
//         */
//        void allotSlot(Jedis redis, List<String> keys) {
//            try {
//                for (String key : keys) {
//                    System.out.println(key);
//                    Integer slot;
//                    slot = JedisClusterCRC16.getSlot(key);
//                    System.out.println(slot);
//                    String s = redis.clusterDelSlots(slot);
//                    String s1 = redis.clusterAddSlots(slot);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        /**
//         * set all keys to allot slot
//         *
//         * @return keys
//         */
//        abstract List<String> setKey();
//
//        abstract void OnMultiAndExecListener(Transaction transaction);
//    }


    /**
     * command : ping
     *
     * @param jedisCluster
     * @return
     */
    static ResultMessage ping(RedisClusterProxy jedisCluster) {
        ResultMessage resultMessage = ResultMessage.buildOK();
        boolean result = true;
        Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
        StringBuilder sb = new StringBuilder();
        for (String node : clusterNodes.keySet()) {
            JedisPool jedisPool = clusterNodes.get(node);
            if (jedisPool != null) {
                Jedis jedis = jedisPool.getResource();
                try {
                    if (!jedis.ping().equals("PONG")) {
                        result = false;
                        sb.append(RedisHelper.getNodeId(jedis.clusterNodes()));
                        sb.append("\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    resultMessage.setResult(false);
                    resultMessage.setInfo(e.toString());
                } finally {
                    jedis.close();//用完一定要close这个链接！！！
                }
            }
        }
        resultMessage.setResult(result);
        if (!result) {
            sb.append("is fail");
            resultMessage.setInfo(sb.toString());
        }
        return resultMessage;
    }

    /**
     * command : random key
     *
     * @param jedisCluster
     * @return key
     */
    static String randomKey(RedisClusterProxy jedisCluster) {
        Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
        Object[] hostAndPorts = clusterNodes.keySet().toArray();
        int randNum = new Random().nextInt(clusterNodes.size() - 1);
        int maxCount = clusterNodes.size() * 6;
        int index = 0;
        while (clusterNodes.get(hostAndPorts[randNum]).getResource().keys("*").size() == 0 && index < maxCount) {
            index++;
            randNum = new Random().nextInt(clusterNodes.size() - 1);
        }
        return clusterNodes.get(hostAndPorts[randNum]).getResource().randomKey();
    }

    /**
     * test known node
     *
     * @param redisClusterProxy
     * @param nodeId
     * @return result
     */
    static boolean isKnownNode(RedisClusterProxy redisClusterProxy, String nodeId) {
        boolean result = false;
        Map<String, JedisPool> clusterNodes = redisClusterProxy.getClusterNodes();

        for (String node : clusterNodes.keySet()) {
            JedisPool jedisPool = clusterNodes.get(node);
            if (jedisPool != null) {
                String nodesInfo = jedisPool.getResource().clusterNodes();
                for (String infoLine : nodesInfo.split("\n")) {
                    for (String info : infoLine.split(" ")) {
                        System.out.println(info);
                        if (info.equals(nodeId)) {
                            result = true;
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * test known node
     *
     * @param jedisCluster
     * @param host
     * @param port
     * @return
     */
    static boolean isKnownNode(RedisClusterProxy jedisCluster, String host, int port) {
        Jedis redis = new Jedis(host, port);
        String nodeId = RedisHelper.getNodeId(redis.clusterNodes());
        return isKnownNode(jedisCluster, nodeId);
    }

    /**
     * command : flushdb
     *
     * @param redisClusterProxy
     * @return
     */
    static boolean flushDB(RedisClusterProxy redisClusterProxy) {
        boolean result = true;
        Map<String, JedisPool> clusterNodes = redisClusterProxy.getClusterNodes();
        for (String node : clusterNodes.keySet()) {
            Jedis redis = null;
            try {
                JedisPool jedisPool = clusterNodes.get(node);
                if (jedisPool != null) {
                    redis = jedisPool.getResource();
                    String nodesInfo = redis.info();
                    if (nodesInfo.contains("role:master")) {
                        redis.flushDB();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = false;
            } finally {
                if (redis != null) {
                    redis.close();
                }
            }
        }
        return result;
    }

    /**
     * command : save
     *
     * @param redisClusterProxy
     * @return
     */
    static boolean save(RedisClusterProxy redisClusterProxy) {
        boolean result = true;
        Map<String, JedisPool> clusterNodes = redisClusterProxy.getClusterNodes();
        for (String node : clusterNodes.keySet()) {
            Jedis redis = null;
            try {
                JedisPool jedisPool = clusterNodes.get(node);
                if (jedisPool != null) {
                    redis = jedisPool.getResource();
                    redis.save();
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = false;
            } finally {
                if (redis != null) {
                    redis.close();
                }
            }
        }
        return result;
    }

    /**
     * command : last save
     *
     * @param redisClusterProxy
     * @return
     */
    static String lastSave(RedisClusterProxy redisClusterProxy) {
        StringBuilder sb = new StringBuilder();
        Map<String, JedisPool> clusterNodes = redisClusterProxy.getClusterNodes();
        for (String node : clusterNodes.keySet()) {
            Jedis redis = null;
            try {
                JedisPool jedisPool = clusterNodes.get(node);
                if (jedisPool != null) {
                    redis = jedisPool.getResource();
                    Long lastSaveTime = redis.lastsave();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String time = format.format(new Date(lastSaveTime));
                    for (String infoLine : redis.clusterNodes().split("\n")) {
                        if (infoLine.contains("myself")) {
                            String[] infos = infoLine.split(" ");
                            sb.append(infos[0]).append("\t").append(infos[1]).append("\t").append(time).append("\n");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (redis != null) {
                    redis.close();
                }
            }

        }
        return sb.toString();
    }

    /**
     * command : background rewrite aof file
     *
     * @param redisClusterProxy
     * @return
     */
    static boolean bgRewriteAof(RedisClusterProxy redisClusterProxy) {
        boolean result = true;
        Map<String, JedisPool> clusterNodes = redisClusterProxy.getClusterNodes();
        for (String node : clusterNodes.keySet()) {
            Jedis redis = null;
            try {
                JedisPool jedisPool = clusterNodes.get(node);
                if (jedisPool != null) {
                    redis = jedisPool.getResource();
                    redis.bgrewriteaof();
                }
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            } finally {
                if (redis != null) {
                    redis.close();
                }
            }
        }
        return result;
    }

    /**
     * canceling the primary server Association, close slave copy. it not allowed in cluster .
     *
     * @param redisClusterProxy
     * @return
     */
    @Deprecated
    static boolean slaveOfNoOne(RedisClusterProxy redisClusterProxy) {
        boolean result = true;
        Map<String, JedisPool> clusterNodes = redisClusterProxy.getClusterNodes();
        for (String node : clusterNodes.keySet()) {
            Jedis redis = null;
            try {
                JedisPool jedisPool = clusterNodes.get(node);
                if (jedisPool != null) {
                    redis = jedisPool.getResource();
                    redis.slaveofNoOne();
                }
            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            } finally {
                if (redis != null) {
                    redis.close();
                }
            }
        }
        return result;
    }

    /**
     * command : background save
     *
     * @param redisClusterProxy
     * @return
     */
    static boolean bgSave(RedisClusterProxy redisClusterProxy) {
        boolean result = true;
        Map<String, JedisPool> clusterNodes = redisClusterProxy.getClusterNodes();
        for (String node : clusterNodes.keySet()) {
            Jedis redis = null;
            try {
                JedisPool jedisPool = clusterNodes.get(node);
                if (jedisPool != null) {
                    redis = jedisPool.getResource();
                    redis.bgsave();
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = false;
            } finally {
                if (redis != null) {
                    redis.close();
                }
            }
        }
        return result;
    }

    /**
     * command : debug
     *
     * @param redisClusterProxy
     * @param pattern
     * @return
     */
    static String debug(RedisClusterProxy redisClusterProxy, String pattern) {
        String result;
        Jedis redis = null;
        try {
            JedisSlotBasedConnectionHandlerProxy connectionHandler = redisClusterProxy.getConnectionHandler();
            int slot = JedisClusterCRC16.getSlot(pattern);
            redis = connectionHandler.getConnectionFromSlot(slot);
            result = redis.debug(DebugParams.OBJECT(pattern));
        } catch (Exception e) {
            e.printStackTrace();
            result = "debug fail ! " + e.getMessage();
        } finally {
            if (redis != null) {
                redis.close();
            }
        }
        return result;
    }


}

