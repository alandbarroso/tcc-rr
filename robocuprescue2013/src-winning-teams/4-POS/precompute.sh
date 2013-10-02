rm -rf LogFiles/*
java -XX:+AggressiveHeap -Xmx25G -Xms2G -jar Poseidon.jar --precompute -at $1 -ac $2 -fb $3 -fs $4 -po $5 -pf $6 -h $7
