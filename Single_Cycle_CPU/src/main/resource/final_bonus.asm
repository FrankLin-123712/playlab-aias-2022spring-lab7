# s0: base address of array
# s1: array size
# s2: the search element
# return index at $a5,if search element not found , put -1 in a5

.data
#success1: .string "found  "
#success2: .string " at index "
#fail_string1: .string "search element "
#fail_string2: .string " not found "
array:   .word 1, 3, 4, 6, 9, 12, 14, 15, 17, 19, 24
array_size: .word 11
search_element: .word 13
.text
main:
    # binarySearch(int arr[], int l, int r, int x) 
    #int l 
    addi t0, zero, 0    
    la   t1, array_size  
    #int r
    lw   t1, 0(t1)      
    addi t1, t1, -1
    # s10 used to check if search is go out of bound of array
    add  s10,t1,zero   
    addi a5, zero, -1
    la   s2, search_element
    #s2 stands for search value
    lw   s2, 0(s2)              
    # load the base address to $s0
    la   s0, array           
    # Jump-and-link to the 'binarySearch' label
    jal  ra, binary_search      
    # (x)bltz a5, Fail				#check if found or not
    blt a5, x0, Fail
    j exit
binary_search:
    # a2 stand for mid = (l + r)/2
    add  a2, t0, t1
    srli a2, a2, 1
    #check if mid > array_size
    blt s10,a2,Fail
    #check if mid < 0
    #(x) bltz a2,Fail
    blt a2, x0, Fail
    # check if array[mid]== search_element
    add  t2, a2, zero
    # t2=t2*4
    slli t2, t2, 2 
    add  t2, t2, s0 
    lw   t2, 0(t2)
    beq  t2, s2, Find
    # check if to == t1 and still not equal
    bge t0,t1,Fail
    #not equal then adjust l,r depends on the value of array[mid]
    blt  s2, t2, less

    #elseif target > array[mid] : l = mid + 1
greater: 
    addi t0,a2,1
    j binary_search

    # @if target<array[mid] : r = mid-1
less: 
    addi t1,a2,-1
    j binary_search
    ret
Fail:
    addi a5, x0, -1
    mv x31, s2
    #addi a5,zero,-1
    #la  a1, fail_string1
    #li  a0, 4
    #ecall

    #mv       a1, s2
    #li       a0, 1
    #ecall

    #la  a1, fail_string2
    #li  a0, 4
    #ecall
    j exit
Find:
    add a5,a2,zero
    #la  a1, success1
    #li  a0, 4
    #ecall

    mv       x31, s2
    #li       a0, 1
    #ecall

    #la  a1, success2
    #li  a0, 4
    #ecall

    #mv       a1, a5
    #li       a0, 1
    #ecall
    
    j exit
exit:
hcf