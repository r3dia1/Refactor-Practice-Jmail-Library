## Code smells & Refactoring
>   1.  Warning & Errors
>   * JRE版本過高導致不支援某些函式，如: assertThat( ) 等。
>   * 使用了某些外部函式庫卻沒有引入。

>   2.  Acyclic Dependencies Priciple
>   ![image](https://github.com/user-attachments/assets/9f9a21d1-142c-4954-8512-bccd7f19e79e)


>   3.  Long method
>   * JMail 提供了 validateInternal( ) 來對 Email 做 RFC 驗證，此方法有高達數百行程式。
>   * 想要對 RFC 驗證方法做Trace，又或是修改維護特定區塊內容時，相當複雜不易。
>   * Solution:
>>  1.  Extract the initial validation checks into a separate method.
>>  2.  Extract the source-routing validation into a separate method.
>>  3.  Extract the main parsing loop into a separate method.
>>  4.  Extract the final validation checks into a separate method.
>>  ![image](https://github.com/user-attachments/assets/8c81944c-e7fc-4b5a-82ae-ed2e7db2f5cc)

>   4.  SRP
>   *   JMail 作為 library 之 interface，不應該完整包含驗證的演算法，違反了 SRP 原則。
應該交給 EmailValidator 存放。


>   5.  Encapulate what varies
>   *   EmailValidator 除了提供基本的RFC驗證，還有提供使用者進行許多客製化的驗證規則。
>   *   為了有效進行驗證規則管理，將各種可新增/刪減的驗證規則放在一個介面(Interface)之後，並成為一個實作(Implementation)，爾後當此實作部份改變時，參考到此介面的其他程式碼部份將不需更改。
>   [image](https://github.com/user-attachments/assets/21729b7b-ec6a-4924-ac7e-496e74a0f478)




>   6.  Violation of open / closed priciple

>   7.  Maguc number
>   *   出現許多硬編碼值，如 320、64、255、63，降低了代碼的可讀性。
>   ![image](https://github.com/user-attachments/assets/a614fdca-a23f-44fe-ad14-5afb518435c2)

