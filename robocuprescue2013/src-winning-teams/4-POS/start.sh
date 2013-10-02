rm -rf LogFiles/*
java -XX:+AggressiveHeap -Xmx6G -Xms6G -jar Poseidon.jar -h $7 -at $1 -ac $2 -fb $3 -fs $4 -po $5 -pf $6
